package com.library.borrow.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectBorrowRequest {
    @NotBlank
    private String reason;
}
