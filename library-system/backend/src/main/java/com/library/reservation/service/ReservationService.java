package com.library.reservation.service;

import com.library.borrow.entity.Reservation;
import com.library.borrow.repository.BorrowSlipItemRepository;
import com.library.borrow.repository.ReservationRepository;
import com.library.catalog.entity.Book;
import com.library.catalog.entity.BookCopy;
import com.library.catalog.repository.BookCopyRepository;
import com.library.catalog.repository.BookRepository;
import com.library.common.enums.CopyStatus;
import com.library.common.enums.ReservationStatus;
import com.library.common.enums.UserRole;
import com.library.common.exception.BusinessException;
import com.library.notification.service.NotificationService;
import com.library.reader.entity.Reader;
import com.library.reader.repository.ReaderRepository;
import com.library.security.AuthUserPrincipal;
import com.library.security.SecurityUtils;
import com.library.settings.service.LibrarySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final ReaderRepository readerRepository;
    private final BorrowSlipItemRepository borrowSlipItemRepository;
    private final LibrarySettingsService settingsService;
    private final NotificationService notificationService;

    public Map<String, Object> create(Long bookId, Long readerId) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        Reader reader = resolveReader(readerId, principal);
        Book book = bookRepository.findByIdAndDeletedAtIsNull(bookId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy sách", HttpStatus.NOT_FOUND));

        long available = bookCopyRepository.countByBook_IdAndStatusAndDeletedAtIsNull(bookId, CopyStatus.AVAILABLE);
        if (available > 0) {
            throw new BusinessException("COPIES_AVAILABLE", "Còn sách sẵn — hãy mượn trực tiếp thay vì đặt trước");
        }

        List<ReservationStatus> active = List.of(ReservationStatus.WAITING, ReservationStatus.READY);
        if (!reservationRepository.findByReader_IdAndBook_IdAndStatusInAndDeletedAtIsNull(reader.getId(), bookId, active).isEmpty()) {
            throw new BusinessException("ALREADY_RESERVED", "Bạn đã đặt trước đầu sách này");
        }
        if (borrowSlipItemRepository.findBorrowedBookIdsByReader(reader.getId()).contains(bookId)) {
            throw new BusinessException("ALREADY_BORROWING", "Đang mượn đầu sách này");
        }

        long current = reservationRepository.countByReader_IdAndStatusInAndDeletedAtIsNull(reader.getId(), active);
        if (current >= reader.getReaderType().getMaxReservations()) {
            throw new BusinessException("RESERVATION_LIMIT", "Vượt số sách đặt trước tối đa");
        }

        int position = reservationRepository.findWaitingQueue(bookId).size() + 1;
        Reservation reservation = new Reservation();
        reservation.setReader(reader);
        reservation.setBook(book);
        reservation.setStatus(ReservationStatus.WAITING);
        reservation.setQueuePosition(position);
        reservation.setCreatedBy(principal.getId());
        reservation.setUpdatedBy(principal.getId());
        reservationRepository.save(reservation);
        return toMap(reservation);
    }

    @Transactional(readOnly = true)
    public PageResult list(Long readerId, int page, int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Reservation> result = readerId != null
                ? reservationRepository.findByReader_IdAndDeletedAtIsNull(readerId, pr)
                : reservationRepository.findByDeletedAtIsNull(pr);
        return new PageResult(result.map(this::toMap).getContent(), result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResult listMine(int page, int size) {
        Reader me = requireMyReader();
        return list(me.getId(), page, size);
    }

    public Map<String, Object> cancel(Long id) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        Reservation reservation = require(id);
        assertAccess(reservation, principal);
        if (reservation.getStatus() != ReservationStatus.WAITING && reservation.getStatus() != ReservationStatus.READY) {
            throw new BusinessException("INVALID_STATUS", "Chỉ hủy khi WAITING/READY");
        }
        if (reservation.getAssignedCopy() != null) {
            BookCopy copy = bookCopyRepository.findByIdForUpdate(reservation.getAssignedCopy().getId()).orElse(null);
            if (copy != null && copy.getStatus() == CopyStatus.HOLD) {
                copy.setStatus(CopyStatus.AVAILABLE);
                bookCopyRepository.save(copy);
            }
        }
        reservation.setStatus(ReservationStatus.CANCELLED);
        reservation.setUpdatedBy(principal.getId());
        reservationRepository.save(reservation);
        promoteNext(reservation.getBook().getId());
        return toMap(reservation);
    }

    public int expireReadyReservations() {
        List<Reservation> expired = reservationRepository.findExpiredReady(Instant.now());
        int n = 0;
        for (Reservation reservation : expired) {
            if (reservation.getAssignedCopy() != null) {
                BookCopy copy = bookCopyRepository.findByIdForUpdate(reservation.getAssignedCopy().getId()).orElse(null);
                if (copy != null && copy.getStatus() == CopyStatus.HOLD) {
                    // sẽ gán cho người kế trong promoteNext
                    copy.setStatus(CopyStatus.AVAILABLE);
                    bookCopyRepository.save(copy);
                }
            }
            reservation.setStatus(ReservationStatus.EXPIRED);
            reservationRepository.save(reservation);
            Long bookId = reservation.getBook().getId();
            Long copyId = reservation.getAssignedCopy() != null ? reservation.getAssignedCopy().getId() : null;
            if (copyId != null) {
                BookCopy copy = bookCopyRepository.findByIdForUpdate(copyId).orElse(null);
                if (copy != null) {
                    promoteWithCopy(bookId, copy);
                } else {
                    promoteNext(bookId);
                }
            } else {
                promoteNext(bookId);
            }
            n++;
        }
        return n;
    }

    private void promoteNext(Long bookId) {
        // no free copy specifically — only used after cancel without copy
    }

    private void promoteWithCopy(Long bookId, BookCopy copy) {
        reservationRepository.findFirstByBook_IdAndStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
                        bookId, ReservationStatus.WAITING)
                .ifPresent(next -> {
                    Instant now = Instant.now();
                    next.setStatus(ReservationStatus.READY);
                    next.setAssignedCopy(copy);
                    next.setReadyAt(now);
                    next.setReadyUntil(now.plusSeconds(settingsService.holdHours() * 3600L));
                    reservationRepository.save(next);
                    copy.setStatus(CopyStatus.HOLD);
                    bookCopyRepository.save(copy);
                    notificationService.notifyUser(
                            next.getReader().getUser().getId(),
                            "RESERVATION_READY",
                            "Sách đặt trước đã sẵn sàng",
                            "Hãy đến nhận trước hạn giữ chỗ.",
                            Map.of("reservationId", next.getId(), "bookId", bookId)
                    );
                });
    }

    private Reader resolveReader(Long readerId, AuthUserPrincipal principal) {
        if (principal.getRole() == UserRole.USER || readerId == null) {
            return requireMyReader();
        }
        return readerRepository.findByIdAndDeletedAtIsNull(readerId)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy độc giả", HttpStatus.NOT_FOUND));
    }

    private Reader requireMyReader() {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        return readerRepository.findByUserIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy hồ sơ độc giả", HttpStatus.NOT_FOUND));
    }

    private Reservation require(Long id) {
        return reservationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy đặt trước", HttpStatus.NOT_FOUND));
    }

    private void assertAccess(Reservation reservation, AuthUserPrincipal principal) {
        if (principal.getRole() == UserRole.ADMIN || principal.getRole() == UserRole.STAFF) {
            return;
        }
        Reader me = requireMyReader();
        if (!Objects.equals(me.getId(), reservation.getReader().getId())) {
            throw new BusinessException("FORBIDDEN", "Không có quyền", HttpStatus.FORBIDDEN);
        }
    }

    private Map<String, Object> toMap(Reservation r) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", r.getId());
        map.put("readerId", r.getReader().getId());
        map.put("readerName", r.getReader().getUser().getFullName());
        map.put("bookId", r.getBook().getId());
        map.put("bookTitle", r.getBook().getTitle());
        map.put("status", r.getStatus());
        map.put("queuePosition", r.getQueuePosition());
        map.put("assignedCopyId", r.getAssignedCopy() != null ? r.getAssignedCopy().getId() : null);
        map.put("barcode", r.getAssignedCopy() != null ? r.getAssignedCopy().getBarcode() : null);
        map.put("readyAt", r.getReadyAt());
        map.put("readyUntil", r.getReadyUntil());
        map.put("createdAt", r.getCreatedAt());
        return map;
    }

    public record PageResult(List<Map<String, Object>> items, int page, int size, long total) {
    }
}
