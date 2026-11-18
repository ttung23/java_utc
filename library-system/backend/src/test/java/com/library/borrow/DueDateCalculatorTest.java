package com.library.borrow;

import com.library.borrow.service.DueDateCalculator;
import com.library.catalog.entity.Book;
import com.library.reader.entity.ReaderType;
import com.library.settings.entity.Holiday;
import com.library.settings.repository.HolidayRepository;
import com.library.settings.service.LibrarySettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DueDateCalculatorTest {

    @Mock
    HolidayRepository holidayRepository;
    @Mock
    LibrarySettingsService settingsService;

    @InjectMocks
    DueDateCalculator calculator;

    @BeforeEach
    void setUp() {
        when(settingsService.weekendDays()).thenReturn(Set.of("SUNDAY"));
        when(holidayRepository.findAll()).thenReturn(List.of());
    }

    @Test
    void skipsSunday() {
        ReaderType type = new ReaderType();
        type.setLoanDays(1);
        Book book = new Book();
        // Saturday + 1 day = Sunday → Monday
        LocalDate due = calculator.calculate(LocalDate.of(2026, 11, 21), type, book);
        assertThat(due).isEqualTo(LocalDate.of(2026, 11, 23));
    }

    @Test
    void usesLoanDaysOverride() {
        ReaderType type = new ReaderType();
        type.setLoanDays(14);
        Book book = new Book();
        book.setLoanDaysOverride(7);
        LocalDate due = calculator.calculate(LocalDate.of(2026, 11, 10), type, book);
        assertThat(due).isEqualTo(LocalDate.of(2026, 11, 17));
    }

    @Test
    void skipsHoliday() {
        Holiday holiday = new Holiday();
        holiday.setDate(LocalDate.of(2026, 11, 18));
        holiday.setName("Test");
        holiday.setRecurring(false);
        when(holidayRepository.findAll()).thenReturn(List.of(holiday));

        LocalDate moved = calculator.skipNonWorkingDays(LocalDate.of(2026, 11, 18));
        assertThat(moved).isEqualTo(LocalDate.of(2026, 11, 19));
    }
}
