package com.library.reader.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateReaderRequest {

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    private String phone;

    @NotNull(message = "Loại độc giả bắt buộc")
    private Long readerTypeId;

    private LocalDate dob;
    private String gender;
    private String address;
    private String idNumber;
    private String studentCode;
    private String organization;
    private String note;
    private String emergencyContact;
}
