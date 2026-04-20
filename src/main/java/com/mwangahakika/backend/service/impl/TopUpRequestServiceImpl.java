package com.mwangahakika.backend.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mwangahakika.backend.dto.CreateTopUpRequestRequest;
import com.mwangahakika.backend.dto.ReviewTopUpRequestRequest;
import com.mwangahakika.backend.dto.TopUpRequestResponse;
import com.mwangahakika.backend.entity.TopUpRequest;
import com.mwangahakika.backend.entity.User;
import com.mwangahakika.backend.entity.Wallet;
import com.mwangahakika.backend.entity.WalletTransaction;
import com.mwangahakika.backend.enums.TopUpRequestStatus;
import com.mwangahakika.backend.enums.TransactionType;
import com.mwangahakika.backend.enums.WalletStatus;
import com.mwangahakika.backend.exception.ResourceNotFoundException;
import com.mwangahakika.backend.exception.UnauthorizedActionException;
import com.mwangahakika.backend.repository.TopUpRequestRepository;
import com.mwangahakika.backend.repository.UserRepository;
import com.mwangahakika.backend.repository.WalletRepository;
import com.mwangahakika.backend.repository.WalletTransactionRepository;
import com.mwangahakika.backend.service.TopUpRequestService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TopUpRequestServiceImpl implements TopUpRequestService {

    private final TopUpRequestRepository topUpRequestRepository;
    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public TopUpRequestResponse create(Long authenticatedUserId, CreateTopUpRequestRequest request) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Top-up amount must be greater than zero.");
        }

        Wallet wallet = walletRepository.findById(request.walletId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + request.walletId()));

        if (!wallet.getUser().getId().equals(authenticatedUserId)) {
            throw new UnauthorizedActionException("You can only request a top-up for your own wallet.");
        }

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active.");
        }

        User user = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authenticatedUserId));

        LocalDateTime now = LocalDateTime.now();

        TopUpRequest topUpRequest = TopUpRequest.builder()
                .user(user)
                .wallet(wallet)
                .amount(request.amount())
                .status(TopUpRequestStatus.PENDING)
                .note(request.note())
                .requestedAt(now)
                .createdAt(now)
                .updatedAt(now)
                .build();

        TopUpRequest saved = topUpRequestRepository.save(topUpRequest);

        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopUpRequestResponse> getMyRequests(Long authenticatedUserId) {
        return topUpRequestRepository.findByUserIdOrderByRequestedAtDesc(authenticatedUserId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopUpRequestResponse> getByStatus(TopUpRequestStatus status) {
        return topUpRequestRepository.findByStatusOrderByRequestedAtAsc(status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public TopUpRequestResponse approve(Long adminUserId, Long requestId, ReviewTopUpRequestRequest request) {
        TopUpRequest topUpRequest = topUpRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Top-up request not found: " + requestId));

        if (topUpRequest.getStatus() != TopUpRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending top-up requests can be approved.");
        }

        Wallet wallet = walletRepository.findByIdForUpdate(topUpRequest.getWallet().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + topUpRequest.getWallet().getId()));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Wallet is not active.");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found: " + adminUserId));

        LocalDateTime now = LocalDateTime.now();
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(topUpRequest.getAmount());
        String reference = generateReference();

        wallet.setBalance(balanceAfter);
        wallet.setUpdatedAt(now);
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .transactionType(TransactionType.TOP_UP)
                .amount(topUpRequest.getAmount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .reference(reference)
                .topUpRequest(topUpRequest)
                .performedBy(admin)
                .description("Approved top-up request " + topUpRequest.getId())
                .createdAt(now)
                .build();

        walletTransactionRepository.save(transaction);

        topUpRequest.setStatus(TopUpRequestStatus.APPROVED);
        topUpRequest.setReviewedBy(admin);
        topUpRequest.setReviewedAt(now);
        topUpRequest.setUpdatedAt(now);

        if (request != null && request.note() != null && !request.note().isBlank()) {
            topUpRequest.setNote(request.note());
        }

        TopUpRequest saved = topUpRequestRepository.save(topUpRequest);

        return toResponse(saved);
    }

    @Override
    @Transactional
    public TopUpRequestResponse reject(Long adminUserId, Long requestId, ReviewTopUpRequestRequest request) {
        TopUpRequest topUpRequest = topUpRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Top-up request not found: " + requestId));

        if (topUpRequest.getStatus() != TopUpRequestStatus.PENDING) {
            throw new IllegalStateException("Only pending top-up requests can be rejected.");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found: " + adminUserId));

        LocalDateTime now = LocalDateTime.now();

        topUpRequest.setStatus(TopUpRequestStatus.REJECTED);
        topUpRequest.setReviewedBy(admin);
        topUpRequest.setReviewedAt(now);
        topUpRequest.setUpdatedAt(now);

        if (request != null && request.note() != null && !request.note().isBlank()) {
            topUpRequest.setNote(request.note());
        }

        TopUpRequest saved = topUpRequestRepository.save(topUpRequest);

        return toResponse(saved);
    }

    private TopUpRequestResponse toResponse(TopUpRequest entity) {
        return new TopUpRequestResponse(
                entity.getId(),
                entity.getUser().getId(),
                entity.getWallet().getId(),
                entity.getAmount(),
                entity.getStatus().name(),
                entity.getNote(),
                entity.getReviewedBy() != null ? entity.getReviewedBy().getId() : null,
                entity.getReviewedAt(),
                entity.getRequestedAt()
        );
    }

    private String generateReference() {
        return "TUP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}