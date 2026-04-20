package com.mwangahakika.backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransferResponse(
        Long transferId,
        String reference,
        String status,
        BigDecimal amount,
        Long senderWalletId,
        Long receiverWalletId,
        LocalDateTime createdAt,
        LocalDateTime completedAt
) {}
