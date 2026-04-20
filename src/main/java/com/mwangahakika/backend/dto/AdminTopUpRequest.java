package com.mwangahakika.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AdminTopUpRequest(
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        String note
) {}
