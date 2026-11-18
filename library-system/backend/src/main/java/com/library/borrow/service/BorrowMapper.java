package com.library.borrow.service;

import com.library.borrow.dto.BorrowSlipResponse;
import com.library.borrow.entity.BorrowSlip;
import com.library.borrow.entity.BorrowSlipItem;
import com.library.common.enums.BorrowItemStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BorrowMapper {

    private final FineCalculator fineCalculator;

    public BorrowSlipResponse toResponse(BorrowSlip slip, List<BorrowSlipItem> items) {
        LocalDate today = LocalDate.now(BorrowPolicyService.ZONE);
        boolean slipOverdue = items.stream().anyMatch(i ->
                i.getStatus() == BorrowItemStatus.BORROWING
                        && i.getDueDate() != null
                        && i.getDueDate().isBefore(today));

        List<BorrowSlipResponse.ItemResponse> itemResponses = items.stream().map(i -> {
            boolean itemOverdue = i.getStatus() == BorrowItemStatus.BORROWING
                    && i.getDueDate() != null
                    && i.getDueDate().isBefore(today);
            BigDecimal estimated = BigDecimal.ZERO;
            if (itemOverdue) {
                estimated = fineCalculator.previewOverdue(i.getDueDate(), today, i.getBook()).amount();
            }
            return BorrowSlipResponse.ItemResponse.builder()
                    .id(i.getId())
                    .bookId(i.getBook().getId())
                    .bookTitle(i.getBook().getTitle())
                    .copyId(i.getCopy() != null ? i.getCopy().getId() : null)
                    .barcode(i.getCopy() != null ? i.getCopy().getBarcode() : null)
                    .status(i.getStatus())
                    .borrowedAt(i.getBorrowedAt())
                    .dueDate(i.getDueDate())
                    .overdue(itemOverdue)
                    .returnedAt(i.getReturnedAt())
                    .renewCount(i.getRenewCount())
                    .conditionOut(i.getConditionOut())
                    .conditionIn(i.getConditionIn())
                    .returnNote(i.getReturnNote())
                    .estimatedFine(estimated)
                    .build();
        }).toList();

        return BorrowSlipResponse.builder()
                .id(slip.getId())
                .slipCode(slip.getSlipCode())
                .readerId(slip.getReader().getId())
                .readerName(slip.getReader().getUser().getFullName())
                .readerCode(slip.getReader().getReaderCode())
                .channel(slip.getChannel())
                .status(slip.getStatus())
                .overdue(slipOverdue)
                .requestedAt(slip.getRequestedAt())
                .approvedAt(slip.getApprovedAt())
                .borrowedAt(slip.getBorrowedAt())
                .pickupDeadline(slip.getPickupDeadline())
                .depositAmount(slip.getDepositAmount())
                .note(slip.getNote())
                .rejectedReason(slip.getRejectedReason())
                .createdAt(slip.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
