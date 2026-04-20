package com.mwangahakika.backend.dto;

public record RegisterRequest(
        String fullName,
        String email,
        String password
) {}
