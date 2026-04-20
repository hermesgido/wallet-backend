package com.mwangahakika.backend.service.impl;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mwangahakika.backend.dto.TransferRequest;
import com.mwangahakika.backend.dto.TransferResponse;
import com.mwangahakika.backend.entity.Transfer;
import com.mwangahakika.backend.entity.User;
import com.mwangahakika.backend.entity.Wallet;
import com.mwangahakika.backend.entity.WalletTransaction;
import com.mwangahakika.backend.enums.TransactionType;
import com.mwangahakika.backend.enums.TransferStatus;
import com.mwangahakika.backend.enums.WalletStatus;
import com.mwangahakika.backend.exception.InsufficientBalanceException;
import com.mwangahakika.backend.exception.ResourceNotFoundException;
import com.mwangahakika.backend.exception.UnauthorizedActionException;
import com.mwangahakika.backend.repository.TransferRepository;
import com.mwangahakika.backend.repository.UserRepository;
import com.mwangahakika.backend.repository.WalletRepository;
import com.mwangahakika.backend.repository.WalletTransactionRepository;
import com.mwangahakika.backend.service.TransferService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransferServiceImpl implements TransferService {

    private static final BigDecimal MIN_TRANSFER_AMOUNT = new BigDecimal("1.00");
    private static final BigDecimal MAX_TRANSFER_AMOUNT = new BigDecimal("1000000.00");

    private final WalletRepository walletRepository;
    private final TransferRepository transferRepository;
    private final WalletTransactionRepository walletTransactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public TransferResponse transfer(Long authenticatedUserId, TransferRequest request) {
        validateRequest(request);

        Long senderId = request.senderWalletId();
        Long receiverId = request.receiverWalletId();

        if (senderId.equals(receiverId)) {
            throw new IllegalArgumentException("Sender and receiver wallets must be different.");
        }

        Long firstLockId = Math.min(senderId, receiverId);
        Long secondLockId = Math.max(senderId, receiverId);

        Wallet firstLocked = walletRepository.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + firstLockId));

        Wallet secondLocked = walletRepository.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new ResourceNotFoundException("Wallet not found: " + secondLockId));

        Wallet senderWallet = senderId.equals(firstLockId) ? firstLocked : secondLocked;
        Wallet receiverWallet = receiverId.equals(firstLockId) ? firstLocked : secondLocked;

        validateWalletOwnership(authenticatedUserId, senderWallet);
        validateWalletStatuses(senderWallet, receiverWallet);
        validateCurrencies(senderWallet, receiverWallet);
        validateTransferAmount(request.amount(), senderWallet.getBalance());

        User initiatedBy = userRepository.findById(authenticatedUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + authenticatedUserId));

        LocalDateTime now = LocalDateTime.now();
        String reference = generateReference();

        BigDecimal senderBefore = senderWallet.getBalance();
        BigDecimal receiverBefore = receiverWallet.getBalance();

        BigDecimal senderAfter = senderBefore.subtract(request.amount());
        BigDecimal receiverAfter = receiverBefore.add(request.amount());

        Transfer transfer = Transfer.builder()
                .senderWallet(senderWallet)
                .receiverWallet(receiverWallet)
                .amount(request.amount())
                .status(TransferStatus.SUCCESS)
                .reference(reference)
                .initiatedBy(initiatedBy)
                .createdAt(now)
                .completedAt(now)
                .build();

        transfer = transferRepository.save(transfer);

        senderWallet.setBalance(senderAfter);
        senderWallet.setUpdatedAt(now);

        receiverWallet.setBalance(receiverAfter);
        receiverWallet.setUpdatedAt(now);

        walletRepository.save(senderWallet);
        walletRepository.save(receiverWallet);

        WalletTransaction senderTx = WalletTransaction.builder()
                .wallet(senderWallet)
                .transactionType(TransactionType.TRANSFER_OUT)
                .amount(request.amount())
                .balanceBefore(senderBefore)
                .balanceAfter(senderAfter)
                .reference(reference)
                .transfer(transfer)
                .performedBy(initiatedBy)
                .description("Transfer to wallet " + receiverWallet.getId())
                .createdAt(now)
                .build();

        WalletTransaction receiverTx = WalletTransaction.builder()
                .wallet(receiverWallet)
                .transactionType(TransactionType.TRANSFER_IN)
                .amount(request.amount())
                .balanceBefore(receiverBefore)
                .balanceAfter(receiverAfter)
                .reference(reference)
                .transfer(transfer)
                .performedBy(initiatedBy)
                .description("Transfer from wallet " + senderWallet.getId())
                .createdAt(now)
                .build();

        walletTransactionRepository.save(senderTx);
        walletTransactionRepository.save(receiverTx);

        return new TransferResponse(
                transfer.getId(),
                transfer.getReference(),
                transfer.getStatus().name(),
                transfer.getAmount(),
                transfer.getSenderWallet().getId(),
                transfer.getReceiverWallet().getId(),
                transfer.getCreatedAt(),
                transfer.getCompletedAt()
        );
    }

    private void validateRequest(TransferRequest request) {
        if (request.amount() == null || request.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero.");
        }

        if (request.senderWalletId() == null || request.receiverWalletId() == null) {
            throw new IllegalArgumentException("Sender and receiver wallet IDs are required.");
        }
    }

    private void validateWalletOwnership(Long authenticatedUserId, Wallet senderWallet) {
        if (!senderWallet.getUser().getId().equals(authenticatedUserId)) {
            throw new UnauthorizedActionException("You can only transfer from your own wallet.");
        }
    }

    private void validateWalletStatuses(Wallet senderWallet, Wallet receiverWallet) {
        if (senderWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Sender wallet is not active.");
        }
        if (receiverWallet.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalStateException("Receiver wallet is not active.");
        }
    }

    private void validateCurrencies(Wallet senderWallet, Wallet receiverWallet) {
        if (!senderWallet.getCurrency().equals(receiverWallet.getCurrency())) {
            throw new IllegalArgumentException("Cross-currency transfers are not supported.");
        }
    }

    private void validateTransferAmount(BigDecimal amount, BigDecimal senderBalance) {
        if (amount.compareTo(MIN_TRANSFER_AMOUNT) < 0) {
            throw new IllegalArgumentException("Transfer amount is below the minimum allowed.");
        }

        if (amount.compareTo(MAX_TRANSFER_AMOUNT) > 0) {
            throw new IllegalArgumentException("Transfer amount exceeds the maximum allowed.");
        }

        if (senderBalance.compareTo(amount) < 0) {
            throw new InsufficientBalanceException("Insufficient wallet balance.");
        }
    }

    private String generateReference() {
        return "TRF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}
