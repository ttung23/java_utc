package com.library.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Tên đăng nhập bắt buộc")
    private String username;

    @NotBlank(message = "Mật khẩu bắt buộc")
    private String password;
}
