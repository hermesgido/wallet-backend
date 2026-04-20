package com.mwangahakika.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TopUpRequestResponse(
        Long id,
        Long userId,
        Long walletId,
        BigDecimal amount,
        String status,
        String note,
        Long reviewedBy,
        LocalDateTime reviewedAt,
        LocalDateTime requestedAt
) {}
