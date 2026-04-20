package com.mwangahakika.backend.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record TransferRequest(
        @NotNull Long senderWalletId,
        @NotNull Long receiverWalletId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount
) {}
