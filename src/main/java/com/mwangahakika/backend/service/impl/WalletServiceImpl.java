package com.mwangahakika.backend.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mwangahakika.backend.dto.AdminTopUpRequest;
import com.mwangahakika.backend.dto.AdminTopUpResponse;
import com.mwangahakika.backend.entity.User;
import com.mwangahakika.backend.entity.Wallet;
import com.mwangahakika.backend.entity.WalletTransaction;
import com.mwangahakika.backend.enums.TransactionType;
import com.mwangahakika.backend.enums.WalletStatus;
import com.mwangahakika.backend.exception.ResourceNotFoundException;
import com.mwangahakika.backend.repository.UserRepository;
import com.mwangahakika.backend.repository.WalletRepository;
import com.mwangahakika.backend.repository.WalletTransactionRepository;
import com.mwangahakika.backend.service.WalletService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public AdminTopUpResponse adminTopUp(Long adminUserId, Long walletId, AdminTopUpRequest request) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("Admin top-up rejected because amount is invalid: adminUserId={}, walletId={}, amount={}",
                    adminUserId, walletId, request.amount());
            throw new IllegalArgumentException("Top-up amount must be greater than zero.");
        }

        Wallet wallet = walletRepository.findByIdForUpdate(walletId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + walletId));

        if (wallet.getStatus() != WalletStatus.ACTIVE) {
            log.warn("Admin top-up rejected because wallet is not active: adminUserId={}, walletId={}, status={}",
                    adminUserId, walletId, wallet.getStatus());
            throw new IllegalStateException("Wallet is not active.");
        }

        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin user not found: " + adminUserId));

        LocalDateTime now = LocalDateTime.now();
        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(request.amount());
        String reference = generateReference();

        wallet.setBalance(balanceAfter);
        wallet.setUpdatedAt(now);
        walletRepository.save(wallet);

        WalletTransaction transaction = WalletTransaction.builder()
                .wallet(wallet)
                .transactionType(TransactionType.TOP_UP)
                .amount(request.amount())
                .balanceBefore(balanceBefore)
                .balanceAfter(balanceAfter)
                .reference(reference)
                .performedBy(admin)
                .description(
                        request.note() != null && !request.note().isBlank()
                                ? request.note()
                                : "Admin top-up"
                )
                .createdAt(now)
                .build();

        walletTransactionRepository.save(transaction);
        log.info("Admin top-up completed: adminUserId={}, walletId={}, amount={}, reference={}",
                adminUserId, wallet.getId(), request.amount(), reference);

        return new AdminTopUpResponse(
                wallet.getId(),
                request.amount(),
                balanceBefore,
                balanceAfter,
                reference,
                now,
                "Wallet top-up completed successfully."
        );
    }

    private String generateReference() {
        return "ADM-TUP-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();
    }
}
