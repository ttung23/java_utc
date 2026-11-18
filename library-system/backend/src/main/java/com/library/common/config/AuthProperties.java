package com.library.common.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "library.auth")
public class AuthProperties {
    private int maxFailedAttempts = 5;
    private int lockMinutes = 15;
    private int resetTokenMinutes = 30;
}
