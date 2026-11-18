package com.library.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DocumentTypeRequest {

    @NotBlank(message = "Mã loại tài liệu không được để trống")
    private String code;

    @NotBlank(message = "Tên loại tài liệu không được để trống")
    private String name;
}
