package com.library.catalog.dto;

import com.library.common.enums.BookStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class BookDetailResponse {
    private Long id;
    private String title;
    private String isbn13;
    private String isbn10;
    private String subtitle;
    private Long publisherId;
    private String publisherName;
    private Integer publishYear;
    private Long languageId;
    private String languageName;
    private Long documentTypeId;
    private String documentTypeName;
    private Integer pages;
    private BigDecimal price;
    private String description;
    private String coverUrl;
    private String classificationCode;
    private List<String> tags;
    private boolean reference;
    private boolean loanable;
    private Integer loanDaysOverride;
    private BookStatus status;
    private BigDecimal avgRating;
    private int ratingCount;
    private int borrowCount;
    private List<Long> authorIds;
    private List<String> authorNames;
    private List<Long> categoryIds;
    private List<String> categoryNames;
    private CopySummary copySummary;
    private Instant createdAt;
    private Instant updatedAt;
}
