package com.library.catalog.dto;

import com.library.common.enums.CopyCondition;
import com.library.common.enums.CopyStatus;
import lombok.Data;

@Data
public class UpdateCopyRequest {
    private CopyStatus status;
    private Long locationId;
    private CopyCondition condition;
    private String note;
}
