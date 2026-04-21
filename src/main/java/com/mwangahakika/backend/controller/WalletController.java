package com.mwangahakika.backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.mwangahakika.backend.dto.AdminTopUpRequest;
import com.mwangahakika.backend.dto.AdminTopUpResponse;
import com.mwangahakika.backend.security.AuthenticatedUser;
import com.mwangahakika.backend.service.WalletService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/wallets")
@RequiredArgsConstructor
@Tag(name = " Wallets Operations", description = "Admin wallet operations")
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/{walletId}/top-up")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Top up a user's wallet directly as admin")
    public AdminTopUpResponse topUp(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long walletId,
            @Valid @RequestBody AdminTopUpRequest request
    ) {
        return walletService.adminTopUp(user.getId(), walletId, request);
    }
}