package com.library.catalog.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AuthorResponse {
    private Long id;
    private String name;
    private String penName;
    private String nationality;
    private Integer birthYear;
    private Integer deathYear;
    private String bio;
    private String photoUrl;
    private Instant createdAt;
    private Instant updatedAt;
}
