package com.library.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class PublisherResponse {
    private Long id;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String website;
    private Instant createdAt;
    private Instant updatedAt;
}
