package com.library.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LanguageRequest {

    @NotBlank(message = "Mã ngôn ngữ không được để trống")
    private String code;

    @NotBlank(message = "Tên ngôn ngữ không được để trống")
    private String name;
}
