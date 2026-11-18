package com.library.borrow.job;

import com.library.borrow.service.BorrowService;
import com.library.reservation.service.ReservationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BorrowJobs {

    private final BorrowService borrowService;
    private final ReservationService reservationService;

    /** SPEC 9.7: expireUnclaimedSlips — mỗi 15 phút */
    @Scheduled(fixedDelayString = "${library.jobs.expire-unclaimed-ms:900000}")
    public void expireUnclaimedSlips() {
        int n = borrowService.expireUnclaimedSlips();
        if (n > 0) {
            log.info("expireUnclaimedSlips: đã hết hạn {} phiếu APPROVED", n);
        }
    }

    /** SPEC 9.7: expireReadyReservations — mỗi 15 phút */
    @Scheduled(fixedDelayString = "${library.jobs.expire-ready-reservation-ms:900000}")
    public void expireReadyReservations() {
        int n = reservationService.expireReadyReservations();
        if (n > 0) {
            log.info("expireReadyReservations: đã hết hạn {} reservation READY", n);
        }
    }
}
