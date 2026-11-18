package com.library.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RefreshRequest {

    @NotBlank(message = "Refresh token bắt buộc")
    private String refreshToken;
}
