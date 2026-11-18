package com.library.borrow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class CounterBorrowRequest {
    @NotNull
    private Long readerId;
    @NotEmpty
    @Valid
    private List<CounterItemRequest> items;
    private String note;
    private BigDecimal deposit;
    private String overrideReason;

    @Data
    public static class CounterItemRequest {
        @NotNull
        private Long copyId;
        private LocalDate dueDate;
    }
}
