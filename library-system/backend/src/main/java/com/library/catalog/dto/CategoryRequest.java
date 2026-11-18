package com.library.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CategoryRequest {

    private String code;

    @NotBlank(message = "Tên thể loại không được để trống")
    private String name;

    private String description;

    private Long parentId;

    private Integer sortOrder;

    private Boolean active;
}
