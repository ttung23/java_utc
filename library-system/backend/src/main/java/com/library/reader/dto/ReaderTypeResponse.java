package com.library.reader.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ReaderTypeResponse {
    private Long id;
    private String code;
    private String name;
    private int maxBooks;
    private int loanDays;
    private int maxRenewals;
    private int renewDays;
    private int maxReservations;
    private BigDecimal cardFee;
    private int cardValidityMonths;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
}
