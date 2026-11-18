package com.library.borrow.service;

import com.library.catalog.entity.Book;
import com.library.common.enums.FineType;
import com.library.settings.service.LibrarySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * BR-25..30: tính phạt trễ / hỏng / mất.
 */
@Component
@RequiredArgsConstructor
public class FineCalculator {

    private final LibrarySettingsService settingsService;
    private final DueDateCalculator dueDateCalculator;

    public OverduePreview previewOverdue(LocalDate dueDate, LocalDate returnDate, Book book) {
        int overdueDays = countOverdueDays(dueDate, returnDate);
        BigDecimal amount = calculateOverdueAmount(overdueDays, book);
        return new OverduePreview(overdueDays, amount);
    }

    public int countOverdueDays(LocalDate dueDate, LocalDate returnDate) {
        if (dueDate == null || returnDate == null || !returnDate.isAfter(dueDate)) {
            return 0;
        }
        int grace = settingsService.graceDays();
        LocalDate effectiveDue = dueDate.plusDays(grace);
        if (!returnDate.isAfter(effectiveDue)) {
            return 0;
        }
        boolean skipHolidays = settingsService.skipHolidaysInFine();
        int days = 0;
        LocalDate cursor = effectiveDue.plusDays(1);
        while (!cursor.isAfter(returnDate)) {
            if (!skipHolidays || !dueDateCalculator.isNonWorking(cursor)) {
                days++;
            }
            cursor = cursor.plusDays(1);
        }
        return days;
    }

    public BigDecimal calculateOverdueAmount(int overdueDays, Book book) {
        if (overdueDays <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal fee = settingsService.overdueFeePerDay().multiply(BigDecimal.valueOf(overdueDays));
        if (settingsService.fineCapByPrice() && book != null && book.getPrice() != null) {
            fee = fee.min(book.getPrice());
        }
        return fee.setScale(0, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateDamageOrLost(FineType type, Book book) {
        BigDecimal price = book != null && book.getPrice() != null ? book.getPrice() : BigDecimal.ZERO;
        return switch (type) {
            case DAMAGED_MINOR -> percentOf(price, settingsService.damagedMinorPercent());
            case DAMAGED_MAJOR -> percentOf(price, settingsService.damagedMajorPercent());
            case LOST -> price.multiply(settingsService.lostBookMultiplier())
                    .add(settingsService.lostProcessingFee())
                    .setScale(0, RoundingMode.HALF_UP);
            default -> BigDecimal.ZERO;
        };
    }

    private BigDecimal percentOf(BigDecimal price, int percent) {
        return price.multiply(BigDecimal.valueOf(percent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
    }

    public record OverduePreview(int overdueDays, BigDecimal amount) {
    }
}
