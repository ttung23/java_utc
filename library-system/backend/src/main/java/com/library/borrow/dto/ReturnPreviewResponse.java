package com.library.borrow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnPreviewResponse {
    private Long itemId;
    private Long slipId;
    private String slipCode;
    private Long readerId;
    private String readerName;
    private Long bookId;
    private String bookTitle;
    private String barcode;
    private LocalDate dueDate;
    private int overdueDays;
    private BigDecimal estimatedFine;
    private int renewCount;
}
