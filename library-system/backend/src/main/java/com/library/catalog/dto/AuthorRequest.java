package com.library.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthorRequest {

    @NotBlank(message = "Tên tác giả không được để trống")
    private String name;

    private String penName;

    private String nationality;

    private Integer birthYear;

    private Integer deathYear;

    private String bio;

    private String photoUrl;
}
