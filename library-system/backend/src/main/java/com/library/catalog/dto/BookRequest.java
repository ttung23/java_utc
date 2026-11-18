package com.library.catalog.dto;

import com.library.common.enums.BookStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BookRequest {

    @NotBlank(message = "Tên sách không được để trống")
    private String title;

    private String isbn13;

    private String isbn10;

    private String subtitle;

    private Long publisherId;

    private Integer publishYear;

    private Long languageId;

    private Long documentTypeId;

    private Integer pages;

    private BigDecimal price;

    private String description;

    private String coverUrl;

    private String classificationCode;

    private List<String> tags;

    private Boolean reference;

    private Boolean loanable;

    private Integer loanDaysOverride;

    private BookStatus status;

    private List<Long> authorIds;

    private List<Long> categoryIds;

    @Min(value = 0, message = "Số bản sao ban đầu không hợp lệ")
    private Integer initialCopies;

    private Long locationId;
}
