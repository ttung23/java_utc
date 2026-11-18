package com.library.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class CategoryResponse {
    private Long id;
    private String code;
    private String name;
    private String description;
    private Long parentId;
    private String parentName;
    private int sortOrder;
    private boolean active;
    private Instant createdAt;
    private Instant updatedAt;
    private List<CategoryResponse> children;
}
