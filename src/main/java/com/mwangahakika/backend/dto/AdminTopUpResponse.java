package com.mwangahakika.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminTopUpResponse(
        Long walletId,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String reference,
        LocalDateTime processedAt,
        String message
) {}