package com.library.borrow.service;

import com.library.borrow.repository.BorrowSlipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class SlipCodeGenerator {

    private static final DateTimeFormatter DAY = DateTimeFormatter.BASIC_ISO_DATE;

    private final BorrowSlipRepository borrowSlipRepository;

    public synchronized String next() {
        String day = LocalDate.now(BorrowPolicyService.ZONE).format(DAY);
        String prefix = "PM-" + day + "-";
        long count = borrowSlipRepository.countBySlipCodePrefix(prefix + "%");
        return prefix + String.format("%04d", count + 1);
    }
}
