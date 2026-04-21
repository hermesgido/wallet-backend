package com.mwangahakika.backend.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Min;

@Validated
@ConfigurationProperties(prefix = "app.rate-limit.login")
public record RateLimitProperties(
        @Min(1) int maxAttempts,
        @Min(1) long windowSeconds
) {
}
