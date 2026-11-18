package com.library.borrow.service;

import com.library.catalog.entity.Book;
import com.library.reader.entity.ReaderType;
import com.library.settings.entity.Holiday;
import com.library.settings.repository.HolidayRepository;
import com.library.settings.service.LibrarySettingsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * BR-09: due_date = borrowDate + loanDays (reader type / book override),
 * rồi dời qua cuối tuần & ngày lễ.
 */
@Component
@RequiredArgsConstructor
public class DueDateCalculator {

    private final HolidayRepository holidayRepository;
    private final LibrarySettingsService settingsService;

    public LocalDate calculate(LocalDate borrowDate, ReaderType readerType, Book book) {
        int loanDays = readerType.getLoanDays();
        if (book != null && book.getLoanDaysOverride() != null && book.getLoanDaysOverride() > 0) {
            loanDays = book.getLoanDaysOverride();
        }
        LocalDate due = borrowDate.plusDays(loanDays);
        return skipNonWorkingDays(due);
    }

    public LocalDate extend(LocalDate currentDue, int renewDays) {
        return skipNonWorkingDays(currentDue.plusDays(renewDays));
    }

    public LocalDate skipNonWorkingDays(LocalDate date) {
        Set<String> weekends = settingsService.weekendDays();
        Set<LocalDate> holidays = loadHolidayDates(date.getYear() - 1, date.getYear() + 1);
        LocalDate cursor = date;
        int guard = 0;
        while (isNonWorking(cursor, weekends, holidays) && guard++ < 60) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }

    public boolean isNonWorking(LocalDate date) {
        return isNonWorking(date, settingsService.weekendDays(), loadHolidayDates(date.getYear() - 1, date.getYear() + 1));
    }

    private boolean isNonWorking(LocalDate date, Set<String> weekends, Set<LocalDate> holidays) {
        if (holidays.contains(date)) {
            return true;
        }
        String dayName = date.getDayOfWeek().name();
        return weekends.stream().anyMatch(w -> w.equalsIgnoreCase(dayName));
    }

    private Set<LocalDate> loadHolidayDates(int fromYear, int toYear) {
        List<Holiday> holidays = holidayRepository.findAll();
        Set<LocalDate> dates = new HashSet<>();
        for (Holiday holiday : holidays) {
            if (holiday.isRecurring()) {
                MonthDay md = MonthDay.from(holiday.getDate());
                for (int y = fromYear; y <= toYear; y++) {
                    dates.add(md.atYear(y));
                }
            } else {
                dates.add(holiday.getDate());
            }
        }
        return dates;
    }

    public static DayOfWeek parseDay(String name) {
        return DayOfWeek.valueOf(name.trim().toUpperCase(Locale.ROOT));
    }
}
