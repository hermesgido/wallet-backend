package com.mwangahakika.backend.controller;

import java.util.List;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.mwangahakika.backend.dto.CreateTopUpRequestRequest;
import com.mwangahakika.backend.dto.TopUpRequestResponse;
import com.mwangahakika.backend.security.AuthenticatedUser;
import com.mwangahakika.backend.service.TopUpRequestService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/top-up-requests")
@RequiredArgsConstructor
@Tag(name = "Top Up Requests", description = "User top-up request operations")
public class TopUpRequestController {

    private final TopUpRequestService topUpRequestService;

    @PostMapping
    @Operation(summary = "Create a wallet top-up request")
    public TopUpRequestResponse create(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody CreateTopUpRequestRequest request
    ) {
        return topUpRequestService.create(user.getId(), request);
    }

    @GetMapping("/my")
    @Operation(summary = "Get my top-up requests")
    public List<TopUpRequestResponse> getMyRequests(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        return topUpRequestService.getMyRequests(user.getId());
    }
}