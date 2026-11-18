package com.library.catalog.dto;

import com.library.common.enums.BookStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class BookListItem {
    private Long id;
    private String title;
    private String isbn13;
    private String publisherName;
    private Integer publishYear;
    private BookStatus status;
    private String coverUrl;
    private long totalCopies;
    private long availableCopies;
    private BigDecimal avgRating;
}
