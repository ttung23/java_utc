package com.library.borrow.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class HandoverRequest {
    @NotEmpty
    private List<String> scannedBarcodes;
}
