package com.library.catalog.dto;

import com.library.common.enums.AcquisitionSource;
import com.library.common.enums.CopyCondition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AddCopiesRequest {

    @NotNull(message = "Số lượng bản sao là bắt buộc")
    @Min(value = 1, message = "Số lượng bản sao phải >= 1")
    private Integer quantity;

    private Long locationId;

    private CopyCondition condition;

    private BigDecimal purchasePrice;

    private AcquisitionSource acquisitionSource;
}
