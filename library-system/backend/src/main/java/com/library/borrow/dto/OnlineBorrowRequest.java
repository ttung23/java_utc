package com.library.borrow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class OnlineBorrowRequest {
    @NotEmpty
    @Valid
    private List<OnlineItemRequest> items;
    private LocalDate pickupDate;
    private String note;

    @Data
    public static class OnlineItemRequest {
        @NotNull
        private Long bookId;
    }
}
