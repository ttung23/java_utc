package com.library.reader.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class IssueCardRequest {

    @NotNull(message = "readerId bắt buộc")
    private Long readerId;
}
