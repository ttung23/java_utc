package com.library.borrow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BorrowValidationResult {
    private boolean valid;
    private int remainingQuota;
    @Builder.Default
    private List<PolicyViolation> violations = new ArrayList<>();
}
