package com.library.borrow.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ReturnRequest {
    @NotEmpty
    @Valid
    private List<ReturnItemRequest> items;
    private Boolean collectPayment;
    private String note;

    @Data
    public static class ReturnItemRequest {
        @NotNull
        private String copyBarcode;
        /** NORMAL | DAMAGED_MINOR | DAMAGED_MAJOR | LOST */
        private String condition = "NORMAL";
        private String note;
        private List<String> images;
        private BigDecimal fineOverride;
    }
}
