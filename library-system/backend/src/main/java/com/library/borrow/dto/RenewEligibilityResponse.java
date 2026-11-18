package com.library.borrow.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RenewEligibilityResponse {
    private boolean eligible;
    private LocalDate currentDueDate;
    private LocalDate newDueDate;
    private int renewCount;
    private int maxRenewals;
    @Builder.Default
    private List<PolicyViolation> reasons = new ArrayList<>();
}
