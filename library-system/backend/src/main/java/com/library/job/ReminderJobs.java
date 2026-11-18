package com.library.job;

import com.library.borrow.entity.BorrowSlipItem;
import com.library.borrow.repository.BorrowSlipItemRepository;
import com.library.borrow.service.BorrowPolicyService;
import com.library.common.enums.CardStatus;
import com.library.notification.service.NotificationService;
import com.library.reader.entity.LibraryCard;
import com.library.reader.repository.LibraryCardRepository;
import com.library.settings.service.LibrarySettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReminderJobs {

    private final BorrowSlipItemRepository borrowSlipItemRepository;
    private final LibraryCardRepository libraryCardRepository;
    private final LibrarySettingsService settingsService;
    private final NotificationService notificationService;

    /** Mỗi ngày 08:00 — nhắc sắp đến hạn + quá hạn */
    @Scheduled(cron = "${library.jobs.reminder-cron:0 0 8 * * *}", zone = "Asia/Ho_Chi_Minh")
    @Transactional
    public void dailyReminders() {
        LocalDate today = LocalDate.now(BorrowPolicyService.ZONE);
        int dueSoonDays = settingsService.dueSoonReminderDays();
        LocalDate dueSoonTo = today.plusDays(dueSoonDays);

        List<BorrowSlipItem> dueSoon = borrowSlipItemRepository.findDueSoonItems(today, dueSoonTo);
        for (BorrowSlipItem item : dueSoon) {
            if (item.getDueDate() != null && item.getDueDate().isBefore(today)) {
                continue;
            }
            Long userId = item.getSlip().getReader().getUser().getId();
            notificationService.notifyUser(
                    userId,
                    "DUE_SOON",
                    "Sách sắp đến hạn trả",
                    "Sách \"" + item.getBook().getTitle() + "\" hạn trả " + item.getDueDate(),
                    Map.of("itemId", item.getId(), "dueDate", String.valueOf(item.getDueDate()))
            );
        }

        List<BorrowSlipItem> overdue = borrowSlipItemRepository.findOverdueItems(today);
        for (BorrowSlipItem item : overdue) {
            Long userId = item.getSlip().getReader().getUser().getId();
            notificationService.notifyUser(
                    userId,
                    "OVERDUE",
                    "Sách quá hạn",
                    "Sách \"" + item.getBook().getTitle() + "\" đã quá hạn từ " + item.getDueDate(),
                    Map.of("itemId", item.getId(), "dueDate", String.valueOf(item.getDueDate()))
            );
        }

        Instant now = Instant.now();
        Instant in30 = now.plus(30, ChronoUnit.DAYS);
        Instant in7 = now.plus(7, ChronoUnit.DAYS);
        List<LibraryCard> cards = libraryCardRepository.findAll().stream()
                .filter(c -> c.getDeletedAt() == null && c.getStatus() == CardStatus.ACTIVE)
                .filter(c -> c.getExpiresAt() != null)
                .toList();
        for (LibraryCard card : cards) {
            Instant exp = card.getExpiresAt();
            if (exp.isBefore(now)) {
                card.setStatus(CardStatus.EXPIRED);
                libraryCardRepository.save(card);
                continue;
            }
            boolean remind30 = !exp.isAfter(in30) && exp.isAfter(in7);
            boolean remind7 = !exp.isAfter(in7);
            if (remind30 || remind7) {
                notificationService.notifyUser(
                        card.getReader().getUser().getId(),
                        "CARD_EXPIRY",
                        "Thẻ thư viện sắp hết hạn",
                        "Thẻ " + card.getCardNumber() + " hết hạn vào " + exp,
                        Map.of("cardId", card.getId())
                );
            }
        }
        log.info("dailyReminders: dueSoon={}, overdue={}, cardsChecked={}",
                dueSoon.size(), overdue.size(), cards.size());
    }
}
