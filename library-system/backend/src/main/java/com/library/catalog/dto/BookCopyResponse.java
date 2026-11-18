package com.library.catalog.dto;

import com.library.common.enums.AcquisitionSource;
import com.library.common.enums.CopyCondition;
import com.library.common.enums.CopyStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class BookCopyResponse {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String barcode;
    private int copyNo;
    private Long locationId;
    private String locationCode;
    private CopyStatus status;
    private CopyCondition condition;
    private BigDecimal purchasePrice;
    private Instant acquiredAt;
    private AcquisitionSource acquisitionSource;
    private String note;
    private Instant createdAt;
    private Instant updatedAt;
}
