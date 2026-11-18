package com.library.borrow.service;

import com.library.borrow.entity.Reservation;
import com.library.borrow.repository.ReservationRepository;
import com.library.catalog.entity.BookCopy;
import com.library.common.enums.CopyStatus;
import com.library.common.enums.ReservationStatus;
import com.library.notification.service.NotificationService;
import com.library.settings.service.LibrarySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReservationQueueService {

    private final ReservationRepository reservationRepository;
    private final LibrarySettingsService settingsService;
    private final NotificationService notificationService;

    /**
     * Khi trả sách: nếu có hàng đợi WAITING → HOLD copy + READY reservation.
     * @return thông báo cho staff (nếu có)
     */
    public Optional<String> assignReturnedCopyToNext(BookCopy copy) {
        Optional<Reservation> next = reservationRepository
                .findFirstByBook_IdAndStatusAndDeletedAtIsNullOrderByCreatedAtAsc(
                        copy.getBook().getId(), ReservationStatus.WAITING);
        if (next.isEmpty()) {
            copy.setStatus(CopyStatus.AVAILABLE);
            return Optional.empty();
        }
        Reservation reservation = next.get();
        Instant now = Instant.now();
        reservation.setStatus(ReservationStatus.READY);
        reservation.setAssignedCopy(copy);
        reservation.setReadyAt(now);
        reservation.setReadyUntil(now.plus(settingsService.holdHours(), ChronoUnit.HOURS));
        reservationRepository.save(reservation);
        copy.setStatus(CopyStatus.HOLD);
        notificationService.notifyUser(
                reservation.getReader().getUser().getId(),
                "RESERVATION_READY",
                "Sách đặt trước đã sẵn sàng",
                "Hãy đến nhận trước hạn giữ chỗ: " + reservation.getReadyUntil(),
                Map.of("reservationId", reservation.getId(), "bookId", copy.getBook().getId())
        );
        return Optional.of("Cuốn " + copy.getBarcode() + " đã có người đặt trước — đặt vào kệ giữ chỗ (READY đến "
                + reservation.getReadyUntil() + ")");
    }

    public boolean hasWaitingOrReady(Long bookId) {
        return reservationRepository.existsActiveQueueForBook(bookId);
    }
}
