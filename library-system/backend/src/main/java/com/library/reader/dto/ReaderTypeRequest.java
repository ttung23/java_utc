package com.library.reader.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ReaderTypeRequest {

    @NotBlank(message = "Mã loại độc giả không được để trống")
    private String code;

    @NotBlank(message = "Tên loại độc giả không được để trống")
    private String name;

    @NotNull
    @Min(0)
    private Integer maxBooks;

    @NotNull
    @Min(1)
    private Integer loanDays;

    @NotNull
    @Min(0)
    private Integer maxRenewals;

    @NotNull
    @Min(1)
    private Integer renewDays;

    @NotNull
    @Min(0)
    private Integer maxReservations;

    @NotNull
    @Min(0)
    private BigDecimal cardFee;

    @NotNull
    @Min(1)
    private Integer cardValidityMonths;

    private Boolean active = true;
}
