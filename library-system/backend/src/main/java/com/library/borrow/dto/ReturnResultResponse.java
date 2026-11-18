package com.library.borrow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnResultResponse {
    private BorrowSlipResponse slip;
    @Builder.Default
    private List<FineLine> fines = new ArrayList<>();
    private BigDecimal totalFine;
    @Builder.Default
    private List<String> reservationNotices = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FineLine {
        private Long fineId;
        private String type;
        private BigDecimal amount;
        private String barcode;
        private String bookTitle;
    }
}
