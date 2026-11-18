package com.library.borrow.dto;

import com.library.common.enums.BorrowChannel;
import com.library.common.enums.BorrowItemStatus;
import com.library.common.enums.BorrowSlipStatus;
import com.library.common.enums.CopyCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowSlipResponse {
    private Long id;
    private String slipCode;
    private Long readerId;
    private String readerName;
    private String readerCode;
    private BorrowChannel channel;
    private BorrowSlipStatus status;
    private boolean overdue;
    private Instant requestedAt;
    private Instant approvedAt;
    private Instant borrowedAt;
    private Instant pickupDeadline;
    private BigDecimal depositAmount;
    private String note;
    private String rejectedReason;
    private Instant createdAt;
    @Builder.Default
    private List<ItemResponse> items = new ArrayList<>();

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemResponse {
        private Long id;
        private Long bookId;
        private String bookTitle;
        private Long copyId;
        private String barcode;
        private BorrowItemStatus status;
        private Instant borrowedAt;
        private LocalDate dueDate;
        private boolean overdue;
        private Instant returnedAt;
        private int renewCount;
        private CopyCondition conditionOut;
        private CopyCondition conditionIn;
        private String returnNote;
        private BigDecimal estimatedFine;
    }
}
