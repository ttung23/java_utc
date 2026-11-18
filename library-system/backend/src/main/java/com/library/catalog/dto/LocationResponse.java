package com.library.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LocationResponse {
    private Long id;
    private String code;
    private String zone;
    private String floor;
    private String row;
    private String shelf;
    private String slot;
    private Integer capacity;
    private String note;
    private Instant createdAt;
    private Instant updatedAt;
}
