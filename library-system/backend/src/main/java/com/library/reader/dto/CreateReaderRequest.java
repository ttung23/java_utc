package com.library.reader.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CreateReaderRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không hợp lệ")
    private String email;

    private String phone;

    /** Mặc định User@123 nếu bỏ trống */
    private String password;

    @NotNull(message = "Loại độc giả bắt buộc")
    private Long readerTypeId;

    private LocalDate dob;
    private String gender;
    private String address;
    private String idNumber;
    private String studentCode;
    private String organization;
    private String note;

    /** Mặc định true — cấp thẻ ngay tại quầy */
    private Boolean issueCard = true;
}
