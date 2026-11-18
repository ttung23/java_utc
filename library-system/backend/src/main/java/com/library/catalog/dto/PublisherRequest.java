package com.library.catalog.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PublisherRequest {

    @NotBlank(message = "Tên nhà xuất bản không được để trống")
    private String name;

    private String address;

    private String phone;

    private String email;

    private String website;
}
