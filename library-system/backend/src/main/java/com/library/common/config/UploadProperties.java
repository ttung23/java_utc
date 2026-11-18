package com.library.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "library.upload")
public class UploadProperties {
    private String dir = "uploads";
    private long maxCoverBytes = 2 * 1024 * 1024;
}
