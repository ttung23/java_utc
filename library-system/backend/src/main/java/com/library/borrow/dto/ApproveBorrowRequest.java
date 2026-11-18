package com.library.borrow.dto;

import jakarta.validation.Valid;
import lombok.Data;

import java.util.List;

@Data
public class ApproveBorrowRequest {
    @Valid
    private List<ApproveItem> items;

    @Data
    public static class ApproveItem {
        private Long itemId;
        private Long copyId;
    }
}
