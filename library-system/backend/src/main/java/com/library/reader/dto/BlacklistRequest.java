package com.library.reader.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BlacklistRequest {

    @NotNull(message = "blacklisted bắt buộc")
    private Boolean blacklisted;

    private String reason;
}
