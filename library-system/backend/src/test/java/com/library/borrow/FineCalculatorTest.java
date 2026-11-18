package com.library.borrow;

import com.library.borrow.service.DueDateCalculator;
import com.library.borrow.service.FineCalculator;
import com.library.catalog.entity.Book;
import com.library.common.enums.FineType;
import com.library.settings.service.LibrarySettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FineCalculatorTest {

    @Mock
    LibrarySettingsService settingsService;
    @Mock
    DueDateCalculator dueDateCalculator;

    @InjectMocks
    FineCalculator fineCalculator;

    @BeforeEach
    void setUp() {
        org.mockito.Mockito.lenient().when(settingsService.graceDays()).thenReturn(0);
        org.mockito.Mockito.lenient().when(settingsService.skipHolidaysInFine()).thenReturn(false);
        org.mockito.Mockito.lenient().when(settingsService.overdueFeePerDay()).thenReturn(BigDecimal.valueOf(2000));
        org.mockito.Mockito.lenient().when(settingsService.fineCapByPrice()).thenReturn(true);
    }

    @Test
    void overdueFiveDays_equals10000() {
        Book book = new Book();
        book.setPrice(BigDecimal.valueOf(100000));
        var preview = fineCalculator.previewOverdue(
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 15),
                book
        );
        assertThat(preview.overdueDays()).isEqualTo(5);
        assertThat(preview.amount()).isEqualByComparingTo("10000");
    }

    @Test
    void overdueCappedByBookPrice() {
        Book book = new Book();
        book.setPrice(BigDecimal.valueOf(5000));
        var preview = fineCalculator.previewOverdue(
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 20),
                book
        );
        assertThat(preview.amount()).isEqualByComparingTo("5000");
    }

    @Test
    void lostIsPriceTimesMultiplierPlusFee() {
        when(settingsService.lostBookMultiplier()).thenReturn(BigDecimal.valueOf(2));
        when(settingsService.lostProcessingFee()).thenReturn(BigDecimal.valueOf(20000));
        Book book = new Book();
        book.setPrice(BigDecimal.valueOf(50000));
        assertThat(fineCalculator.calculateDamageOrLost(FineType.LOST, book))
                .isEqualByComparingTo("120000");
    }
}
