package com.library.borrow.dto;

import lombok.Data;

import java.util.List;

@Data
public class ValidateBorrowRequest {
    private Long readerId;
    private List<Long> copyIds;
    private List<Long> bookIds;
}
