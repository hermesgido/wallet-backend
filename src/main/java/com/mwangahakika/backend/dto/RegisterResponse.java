package com.mwangahakika.backend.dto;

public record RegisterResponse(
        Long userId,
        String email,
        Long walletId
) {}
