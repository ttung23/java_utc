package com.library.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank(message = "Họ tên bắt buộc")
    @Size(max = 255)
    private String fullName;

    @NotBlank(message = "Email bắt buộc")
    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(max = 30)
    private String phone;

    @NotBlank(message = "Mật khẩu bắt buộc")
    private String password;

    private String readerTypeCode;
    private LocalDate dob;
    private String address;
    private String idNumber;
    private String studentCode;
}
