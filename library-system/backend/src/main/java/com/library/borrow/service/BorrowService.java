package com.library.borrow.service;

import com.library.borrow.dto.ApproveBorrowRequest;
import com.library.borrow.dto.BorrowSlipResponse;
import com.library.borrow.dto.BorrowValidationResult;
import com.library.borrow.dto.CancelBorrowRequest;
import com.library.borrow.dto.CounterBorrowRequest;
import com.library.borrow.dto.HandoverRequest;
import com.library.borrow.dto.OnlineBorrowRequest;
import com.library.borrow.dto.RejectBorrowRequest;
import com.library.borrow.dto.ValidateBorrowRequest;
import com.library.borrow.entity.BorrowSlip;
import com.library.borrow.entity.BorrowSlipItem;
import com.library.borrow.entity.Reservation;
import com.library.borrow.repository.BorrowSlipItemRepository;
import com.library.borrow.repository.BorrowSlipRepository;
import com.library.borrow.repository.ReservationRepository;
import com.library.catalog.entity.Book;
import com.library.catalog.entity.BookCopy;
import com.library.catalog.repository.BookCopyRepository;
import com.library.catalog.repository.BookRepository;
import com.library.common.enums.BorrowChannel;
import com.library.common.enums.BorrowItemStatus;
import com.library.common.enums.BorrowSlipStatus;
import com.library.common.enums.CopyStatus;
import com.library.common.enums.ReservationStatus;
import com.library.common.enums.UserRole;
import com.library.common.exception.BusinessException;
import com.library.reader.entity.Reader;
import com.library.reader.repository.ReaderRepository;
import com.library.security.AuthUserPrincipal;
import com.library.security.SecurityUtils;
import com.library.settings.service.LibrarySettingsService;
import com.library.user.entity.User;
import com.library.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class BorrowService {

    private final BorrowSlipRepository borrowSlipRepository;
    private final BorrowSlipItemRepository borrowSlipItemRepository;
    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final ReaderRepository readerRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;
    private final BorrowPolicyService borrowPolicyService;
    private final DueDateCalculator dueDateCalculator;
    private final FineCalculator fineCalculator;
    private final SlipCodeGenerator slipCodeGenerator;
    private final LibrarySettingsService settingsService;
    private final BorrowMapper borrowMapper;

    public BorrowValidationResult validate(ValidateBorrowRequest request) {
        Reader reader = resolveReaderForValidate(request.getReaderId());
        if (request.getBookIds() != null && !request.getBookIds().isEmpty()) {
            List<Book> books = request.getBookIds().stream().map(this::requireBook).toList();
            return borrowPolicyService.validateOnlineBooks(reader, books);
        }
        List<BookCopy> copies = new ArrayList<>();
        if (request.getCopyIds() != null) {
            for (Long copyId : request.getCopyIds()) {
                copies.add(requireCopy(copyId));
            }
        }
        return borrowPolicyService.validateBorrow(reader, copies);
    }

    public BorrowSlipResponse createCounter(CounterBorrowRequest request) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        User staff = requireUser(principal.getId());
        Reader reader = requireReader(request.getReaderId());

        List<BookCopy> lockedCopies = new ArrayList<>();
        for (CounterBorrowRequest.CounterItemRequest item : request.getItems()) {
            BookCopy copy = bookCopyRepository.findByIdForUpdate(item.getCopyId())
                    .orElseThrow(() -> new BusinessException("COPY_NOT_FOUND", "Không tìm thấy bản sao", HttpStatus.NOT_FOUND));
            lockedCopies.add(copy);
        }

        BorrowValidationResult validation = borrowPolicyService.validateBorrow(reader, lockedCopies);
        if (!validation.isValid()) {
            boolean canOverride = principal.getRole() == UserRole.ADMIN
                    && StringUtils.hasText(request.getOverrideReason());
            if (!canOverride) {
                throw new BusinessException("BORROW_NOT_ALLOWED", "Không đủ điều kiện mượn",
                        HttpStatus.BAD_REQUEST, validation.getViolations());
            }
        }

        Instant now = Instant.now();
        LocalDate today = LocalDate.now(BorrowPolicyService.ZONE);

        BorrowSlip slip = new BorrowSlip();
        slip.setSlipCode(slipCodeGenerator.next());
        slip.setReader(reader);
        slip.setChannel(BorrowChannel.COUNTER);
        slip.setStatus(BorrowSlipStatus.BORROWING);
        slip.setRequestedAt(now);
        slip.setBorrowedAt(now);
        slip.setIssuedBy(staff);
        slip.setDepositAmount(request.getDeposit() != null ? request.getDeposit() : BigDecimal.ZERO);
        slip.setNote(request.getNote());
        slip.setCreatedBy(staff.getId());
        slip.setUpdatedBy(staff.getId());
        borrowSlipRepository.save(slip);

        List<BorrowSlipItem> items = new ArrayList<>();
        for (int i = 0; i < request.getItems().size(); i++) {
            CounterBorrowRequest.CounterItemRequest reqItem = request.getItems().get(i);
            BookCopy copy = lockedCopies.get(i);
            Book book = copy.getBook();

            LocalDate due = reqItem.getDueDate() != null
                    ? dueDateCalculator.skipNonWorkingDays(reqItem.getDueDate())
                    : dueDateCalculator.calculate(today, reader.getReaderType(), book);

            BorrowSlipItem item = new BorrowSlipItem();
            item.setSlip(slip);
            item.setBook(book);
            item.setCopy(copy);
            item.setStatus(BorrowItemStatus.BORROWING);
            item.setBorrowedAt(now);
            item.setDueDate(due);
            item.setConditionOut(copy.getCondition());
            item.setCreatedBy(staff.getId());
            item.setUpdatedBy(staff.getId());
            borrowSlipItemRepository.save(item);
            items.add(item);

            copy.setStatus(CopyStatus.BORROWED);
            bookCopyRepository.save(copy);
            book.setBorrowCount(book.getBorrowCount() + 1);
            bookRepository.save(book);

            fulfillReservationIfAny(reader.getId(), book.getId(), slip, copy);
        }

        return borrowMapper.toResponse(slip, items);
    }

    public BorrowSlipResponse createOnline(OnlineBorrowRequest request) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        Reader reader = readerRepository.findByUserIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy hồ sơ độc giả", HttpStatus.NOT_FOUND));

        List<Book> books = request.getItems().stream()
                .map(i -> requireBook(i.getBookId()))
                .toList();
        BorrowValidationResult validation = borrowPolicyService.validateOnlineBooks(reader, books);
        if (!validation.isValid()) {
            throw new BusinessException("BORROW_NOT_ALLOWED", "Không đủ điều kiện gửi yêu cầu",
                    HttpStatus.BAD_REQUEST, validation.getViolations());
        }

        Instant now = Instant.now();
        BorrowSlip slip = new BorrowSlip();
        slip.setSlipCode(slipCodeGenerator.next());
        slip.setReader(reader);
        slip.setChannel(BorrowChannel.ONLINE);
        slip.setStatus(BorrowSlipStatus.PENDING);
        slip.setRequestedAt(now);
        slip.setNote(request.getNote());
        slip.setDepositAmount(BigDecimal.ZERO);
        slip.setCreatedBy(principal.getId());
        slip.setUpdatedBy(principal.getId());
        borrowSlipRepository.save(slip);

        List<BorrowSlipItem> items = new ArrayList<>();
        for (Book book : books) {
            BorrowSlipItem item = new BorrowSlipItem();
            item.setSlip(slip);
            item.setBook(book);
            item.setStatus(BorrowItemStatus.REQUESTED);
            item.setCreatedBy(principal.getId());
            item.setUpdatedBy(principal.getId());
            borrowSlipItemRepository.save(item);
            items.add(item);
        }
        return borrowMapper.toResponse(slip, items);
    }

    public BorrowSlipResponse approve(Long slipId, ApproveBorrowRequest request) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        User staff = requireUser(principal.getId());
        BorrowSlip slip = requireSlip(slipId);
        if (slip.getStatus() != BorrowSlipStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Chỉ duyệt phiếu PENDING");
        }

        List<BorrowSlipItem> items = borrowSlipItemRepository.findBySlip_IdAndDeletedAtIsNullOrderByIdAsc(slipId);
        Map<Long, Long> copyOverrides = new HashMap<>();
        if (request != null && request.getItems() != null) {
            for (ApproveBorrowRequest.ApproveItem ai : request.getItems()) {
                if (ai.getItemId() != null && ai.getCopyId() != null) {
                    copyOverrides.put(ai.getItemId(), ai.getCopyId());
                }
            }
        }

        Instant now = Instant.now();
        for (BorrowSlipItem item : items) {
            if (item.getStatus() != BorrowItemStatus.REQUESTED) {
                continue;
            }
            Long preferredCopyId = copyOverrides.get(item.getId());
            BookCopy copy;
            if (preferredCopyId != null) {
                copy = bookCopyRepository.findByIdForUpdate(preferredCopyId)
                        .orElseThrow(() -> new BusinessException("COPY_NOT_FOUND", "Không tìm thấy bản sao", HttpStatus.NOT_FOUND));
                if (!Objects.equals(copy.getBook().getId(), item.getBook().getId())) {
                    throw new BusinessException("COPY_BOOK_MISMATCH", "Bản sao không thuộc đầu sách yêu cầu");
                }
                if (copy.getStatus() != CopyStatus.AVAILABLE) {
                    throw new BusinessException("COPY_NOT_AVAILABLE", "Bản sao không sẵn sàng: " + copy.getBarcode());
                }
            } else {
                List<BookCopy> available = bookCopyRepository.findAvailableByBookId(item.getBook().getId());
                if (available.isEmpty()) {
                    throw new BusinessException("NO_AVAILABLE_COPY",
                            "Hết bản sao cho sách: " + item.getBook().getTitle());
                }
                copy = bookCopyRepository.findByIdForUpdate(available.get(0).getId())
                        .orElseThrow(() -> new BusinessException("COPY_NOT_FOUND", "Không tìm thấy bản sao", HttpStatus.NOT_FOUND));
            }
            copy.setStatus(CopyStatus.HOLD);
            bookCopyRepository.save(copy);
            item.setCopy(copy);
            item.setStatus(BorrowItemStatus.HOLD);
            item.setUpdatedBy(staff.getId());
            borrowSlipItemRepository.save(item);
        }

        slip.setStatus(BorrowSlipStatus.APPROVED);
        slip.setApprovedAt(now);
        slip.setApprovedBy(staff);
        slip.setPickupDeadline(now.plus(settingsService.holdHours(), ChronoUnit.HOURS));
        slip.setUpdatedBy(staff.getId());
        borrowSlipRepository.save(slip);
        return getDetail(slipId);
    }

    public BorrowSlipResponse reject(Long slipId, RejectBorrowRequest request) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        BorrowSlip slip = requireSlip(slipId);
        if (slip.getStatus() != BorrowSlipStatus.PENDING) {
            throw new BusinessException("INVALID_STATUS", "Chỉ từ chối phiếu PENDING");
        }
        List<BorrowSlipItem> items = borrowSlipItemRepository.findBySlip_IdAndDeletedAtIsNullOrderByIdAsc(slipId);
        for (BorrowSlipItem item : items) {
            item.setStatus(BorrowItemStatus.CANCELLED);
            item.setUpdatedBy(principal.getId());
            borrowSlipItemRepository.save(item);
        }
        slip.setStatus(BorrowSlipStatus.REJECTED);
        slip.setRejectedReason(request.getReason());
        slip.setUpdatedBy(principal.getId());
        borrowSlipRepository.save(slip);
        return getDetail(slipId);
    }

    public BorrowSlipResponse cancel(Long slipId, CancelBorrowRequest request) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        BorrowSlip slip = requireSlip(slipId);
        assertCanAccessSlip(slip, principal);

        if (slip.getStatus() != BorrowSlipStatus.PENDING && slip.getStatus() != BorrowSlipStatus.APPROVED) {
            throw new BusinessException("INVALID_STATUS", "Chỉ hủy khi PENDING hoặc APPROVED");
        }

        List<BorrowSlipItem> items = borrowSlipItemRepository.findBySlip_IdAndDeletedAtIsNullOrderByIdAsc(slipId);
        for (BorrowSlipItem item : items) {
            if (item.getCopy() != null && (item.getStatus() == BorrowItemStatus.HOLD
                    || item.getCopy().getStatus() == CopyStatus.HOLD)) {
                BookCopy copy = bookCopyRepository.findByIdForUpdate(item.getCopy().getId()).orElse(item.getCopy());
                copy.setStatus(CopyStatus.AVAILABLE);
                bookCopyRepository.save(copy);
            }
            item.setStatus(BorrowItemStatus.CANCELLED);
            item.setUpdatedBy(principal.getId());
            borrowSlipItemRepository.save(item);
        }
        slip.setStatus(BorrowSlipStatus.CANCELLED);
        slip.setCancelledAt(Instant.now());
        slip.setCancelledBy(requireUser(principal.getId()));
        slip.setCancelReason(request != null ? request.getReason() : null);
        slip.setUpdatedBy(principal.getId());
        borrowSlipRepository.save(slip);
        return getDetail(slipId);
    }

    public BorrowSlipResponse handover(Long slipId, HandoverRequest request) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        User staff = requireUser(principal.getId());
        BorrowSlip slip = requireSlip(slipId);
        if (slip.getStatus() != BorrowSlipStatus.APPROVED) {
            throw new BusinessException("INVALID_STATUS", "Chỉ giao sách khi phiếu APPROVED");
        }

        List<BorrowSlipItem> items = borrowSlipItemRepository.findBySlip_IdAndDeletedAtIsNullOrderByIdAsc(slipId);
        Set<String> scanned = request.getScannedBarcodes().stream()
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(String::toUpperCase)
                .collect(Collectors.toCollection(HashSet::new));

        Instant now = Instant.now();
        LocalDate today = LocalDate.now(BorrowPolicyService.ZONE);

        for (BorrowSlipItem item : items) {
            if (item.getStatus() != BorrowItemStatus.HOLD || item.getCopy() == null) {
                continue;
            }
            String barcode = item.getCopy().getBarcode().toUpperCase();
            if (!scanned.contains(barcode)) {
                throw new BusinessException("BARCODE_MISMATCH",
                        "Thiếu quét barcode: " + item.getCopy().getBarcode());
            }
            BookCopy copy = bookCopyRepository.findByIdForUpdate(item.getCopy().getId())
                    .orElseThrow(() -> new BusinessException("COPY_NOT_FOUND", "Không tìm thấy bản sao", HttpStatus.NOT_FOUND));
            copy.setStatus(CopyStatus.BORROWED);
            bookCopyRepository.save(copy);

            item.setStatus(BorrowItemStatus.BORROWING);
            item.setBorrowedAt(now);
            item.setDueDate(dueDateCalculator.calculate(today, slip.getReader().getReaderType(), item.getBook()));
            item.setConditionOut(copy.getCondition());
            item.setUpdatedBy(staff.getId());
            borrowSlipItemRepository.save(item);

            Book book = item.getBook();
            book.setBorrowCount(book.getBorrowCount() + 1);
            bookRepository.save(book);
            fulfillReservationIfAny(slip.getReader().getId(), book.getId(), slip, copy);
        }

        slip.setStatus(BorrowSlipStatus.BORROWING);
        slip.setBorrowedAt(now);
        slip.setIssuedBy(staff);
        slip.setUpdatedBy(staff.getId());
        borrowSlipRepository.save(slip);
        return getDetail(slipId);
    }

    @Transactional(readOnly = true)
    public BorrowSlipResponse getDetail(Long slipId) {
        BorrowSlip slip = requireSlip(slipId);
        assertCanAccessSlip(slip, SecurityUtils.requireCurrentUser());
        List<BorrowSlipItem> items = borrowSlipItemRepository.findBySlip_IdAndDeletedAtIsNullOrderByIdAsc(slipId);
        return borrowMapper.toResponse(slip, items);
    }

    @Transactional(readOnly = true)
    public PageResult list(BorrowSlipStatus status, BorrowChannel channel, Long readerId,
                           Boolean overdueOnly, int page, int size) {
        Specification<BorrowSlip> spec = (root, query, cb) -> cb.isNull(root.get("deletedAt"));
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (channel != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("channel"), channel));
        }
        if (readerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("reader").get("id"), readerId));
        }

        Page<BorrowSlip> result = borrowSlipRepository.findAll(
                spec, PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                        Sort.by(Sort.Direction.DESC, "createdAt")));

        LocalDate today = LocalDate.now(BorrowPolicyService.ZONE);
        List<BorrowSlipResponse> data = new ArrayList<>();
        for (BorrowSlip slip : result.getContent()) {
            List<BorrowSlipItem> items = borrowSlipItemRepository.findBySlip_IdAndDeletedAtIsNullOrderByIdAsc(slip.getId());
            BorrowSlipResponse resp = borrowMapper.toResponse(slip, items);
            if (Boolean.TRUE.equals(overdueOnly) && !resp.isOverdue()) {
                continue;
            }
            data.add(resp);
        }
        // nếu lọc overdue trên page hiện tại — meta vẫn theo page gốc (đơn giản phase này)
        return new PageResult(data, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public PageResult listMine(int page, int size) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        Reader reader = readerRepository.findByUserIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy hồ sơ độc giả", HttpStatus.NOT_FOUND));
        Page<BorrowSlip> result = borrowSlipRepository.findByReader_IdAndDeletedAtIsNullOrderByCreatedAtDesc(
                reader.getId(), PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100)));
        List<BorrowSlipResponse> data = result.getContent().stream().map(slip -> {
            List<BorrowSlipItem> items = borrowSlipItemRepository.findBySlip_IdAndDeletedAtIsNullOrderByIdAsc(slip.getId());
            return borrowMapper.toResponse(slip, items);
        }).toList();
        return new PageResult(data, result.getNumber(), result.getSize(), result.getTotalElements());
    }

    @Transactional(readOnly = true)
    public List<BorrowSlipResponse.ItemResponse> listOverdue() {
        LocalDate today = LocalDate.now(BorrowPolicyService.ZONE);
        return borrowSlipItemRepository.findOverdueItems(today).stream().map(i -> {
            var preview = fineCalculator.previewOverdue(i.getDueDate(), today, i.getBook());
            return BorrowSlipResponse.ItemResponse.builder()
                    .id(i.getId())
                    .bookId(i.getBook().getId())
                    .bookTitle(i.getBook().getTitle())
                    .copyId(i.getCopy() != null ? i.getCopy().getId() : null)
                    .barcode(i.getCopy() != null ? i.getCopy().getBarcode() : null)
                    .status(i.getStatus())
                    .borrowedAt(i.getBorrowedAt())
                    .dueDate(i.getDueDate())
                    .overdue(true)
                    .renewCount(i.getRenewCount())
                    .estimatedFine(preview.amount())
                    .slipId(i.getSlip().getId())
                    .slipCode(i.getSlip().getSlipCode())
                    .readerId(i.getSlip().getReader().getId())
                    .readerName(i.getSlip().getReader().getUser().getFullName())
                    .readerCode(i.getSlip().getReader().getReaderCode())
                    .overdueDays(preview.overdueDays())
                    .build();
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<BorrowSlipResponse.ItemResponse> listDueSoon() {
        LocalDate today = LocalDate.now(BorrowPolicyService.ZONE);
        LocalDate to = today.plusDays(settingsService.dueSoonReminderDays());
        return borrowSlipItemRepository.findDueSoonItems(today, to).stream()
                .filter(i -> i.getDueDate() != null && !i.getDueDate().isBefore(today))
                .map(i -> BorrowSlipResponse.ItemResponse.builder()
                        .id(i.getId())
                        .bookId(i.getBook().getId())
                        .bookTitle(i.getBook().getTitle())
                        .copyId(i.getCopy() != null ? i.getCopy().getId() : null)
                        .barcode(i.getCopy() != null ? i.getCopy().getBarcode() : null)
                        .status(i.getStatus())
                        .borrowedAt(i.getBorrowedAt())
                        .dueDate(i.getDueDate())
                        .overdue(false)
                        .renewCount(i.getRenewCount())
                        .estimatedFine(BigDecimal.ZERO)
                        .build())
                .toList();
    }

    public int expireUnclaimedSlips() {
        List<BorrowSlip> expired = borrowSlipRepository.findExpiredUnclaimed(Instant.now());
        int count = 0;
        for (BorrowSlip slip : expired) {
            List<BorrowSlipItem> items = borrowSlipItemRepository.findBySlip_IdAndDeletedAtIsNullOrderByIdAsc(slip.getId());
            for (BorrowSlipItem item : items) {
                if (item.getCopy() != null) {
                    BookCopy copy = bookCopyRepository.findByIdForUpdate(item.getCopy().getId()).orElse(null);
                    if (copy != null && copy.getStatus() == CopyStatus.HOLD) {
                        copy.setStatus(CopyStatus.AVAILABLE);
                        bookCopyRepository.save(copy);
                    }
                }
                item.setStatus(BorrowItemStatus.EXPIRED);
                borrowSlipItemRepository.save(item);
            }
            slip.setStatus(BorrowSlipStatus.EXPIRED);
            borrowSlipRepository.save(slip);
            Reader reader = slip.getReader();
            reader.setNoShowCount(reader.getNoShowCount() + 1);
            readerRepository.save(reader);
            count++;
        }
        return count;
    }

    private void fulfillReservationIfAny(Long readerId, Long bookId, BorrowSlip slip, BookCopy copy) {
        List<Reservation> ready = reservationRepository.findByReader_IdAndBook_IdAndStatusInAndDeletedAtIsNull(
                readerId, bookId, List.of(ReservationStatus.READY, ReservationStatus.WAITING));
        for (Reservation reservation : ready) {
            if (reservation.getStatus() == ReservationStatus.READY
                    || (reservation.getAssignedCopy() != null
                    && Objects.equals(reservation.getAssignedCopy().getId(), copy.getId()))) {
                reservation.setStatus(ReservationStatus.FULFILLED);
                reservation.setFulfilledSlip(slip);
                reservationRepository.save(reservation);
            }
        }
    }

    private void assertCanAccessSlip(BorrowSlip slip, AuthUserPrincipal principal) {
        if (principal.getRole() == UserRole.ADMIN || principal.getRole() == UserRole.STAFF) {
            return;
        }
        Reader reader = readerRepository.findByUserIdAndDeletedAtIsNull(principal.getId()).orElse(null);
        if (reader == null || !Objects.equals(reader.getId(), slip.getReader().getId())) {
            throw new BusinessException("FORBIDDEN", "Không có quyền xem phiếu này", HttpStatus.FORBIDDEN);
        }
    }

    private Reader resolveReaderForValidate(Long readerId) {
        AuthUserPrincipal principal = SecurityUtils.requireCurrentUser();
        if (readerId != null) {
            if (principal.getRole() == UserRole.USER) {
                Reader me = readerRepository.findByUserIdAndDeletedAtIsNull(principal.getId())
                        .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy hồ sơ độc giả", HttpStatus.NOT_FOUND));
                if (!Objects.equals(me.getId(), readerId)) {
                    throw new BusinessException("FORBIDDEN", "Không được kiểm tra độc giả khác", HttpStatus.FORBIDDEN);
                }
                return me;
            }
            return requireReader(readerId);
        }
        return readerRepository.findByUserIdAndDeletedAtIsNull(principal.getId())
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy hồ sơ độc giả", HttpStatus.NOT_FOUND));
    }

    private BorrowSlip requireSlip(Long id) {
        return borrowSlipRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy phiếu mượn", HttpStatus.NOT_FOUND));
    }

    private Reader requireReader(Long id) {
        return readerRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy độc giả", HttpStatus.NOT_FOUND));
    }

    private BookCopy requireCopy(Long id) {
        return bookCopyRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("COPY_NOT_FOUND", "Không tìm thấy bản sao", HttpStatus.NOT_FOUND));
    }

    private Book requireBook(Long id) {
        return bookRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy đầu sách", HttpStatus.NOT_FOUND));
    }

    private User requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException("NOT_FOUND", "Không tìm thấy người dùng", HttpStatus.NOT_FOUND));
    }

    public record PageResult(List<BorrowSlipResponse> items, int page, int size, long total) {
    }
}
