package com.library.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LanguageResponse {
    private Long id;
    private String code;
    private String name;
    private Instant createdAt;
    private Instant updatedAt;
}
