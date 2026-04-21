package com.mwangahakika.backend.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties(prefix = "app.transfer")
public record TransferProperties(
        @NotNull @DecimalMin(value = "0.01") BigDecimal minAmount,
        @NotNull @DecimalMin(value = "0.01") BigDecimal maxAmount
) {
    public TransferProperties {
        if (maxAmount.compareTo(minAmount) < 0) {
            throw new IllegalArgumentException("app.transfer.max-amount must be greater than or equal to min-amount");
        }
    }
}
