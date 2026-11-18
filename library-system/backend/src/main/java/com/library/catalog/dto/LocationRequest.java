package com.library.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LocationRequest {

    @NotBlank(message = "Mã vị trí không được để trống")
    private String code;

    private String zone;

    private String floor;

    private String row;

    private String shelf;

    private String slot;

    private Integer capacity;

    private String note;
}
