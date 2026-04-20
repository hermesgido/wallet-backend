package com.mwangahakika.backend.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mwangahakika.backend.dto.ReviewTopUpRequestRequest;
import com.mwangahakika.backend.dto.TopUpRequestResponse;
import com.mwangahakika.backend.enums.TopUpRequestStatus;
import com.mwangahakika.backend.security.AuthenticatedUser;
import com.mwangahakika.backend.service.TopUpRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin/top-up-requests")
@RequiredArgsConstructor
@Tag(name = "Admin Top Up Requests", description = "Admin review of top-up requests")
public class AdminTopUpRequestController {

    private final TopUpRequestService topUpRequestService;

    @GetMapping
    @Operation(summary = "Get top-up requests by status")
    public List<TopUpRequestResponse> getByStatus(
            @RequestParam(defaultValue = "PENDING") TopUpRequestStatus status
    ) {
        return topUpRequestService.getByStatus(status);
    }

    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve a top-up request")
    public TopUpRequestResponse approve(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @RequestBody(required = false) ReviewTopUpRequestRequest request
    ) {
        return topUpRequestService.approve(user.getId(), id, request);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject a top-up request")
    public TopUpRequestResponse reject(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable Long id,
            @RequestBody(required = false) ReviewTopUpRequestRequest request
    ) {
        return topUpRequestService.reject(user.getId(), id, request);
    }
}