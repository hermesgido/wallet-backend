package com.mwangahakika.backend;


import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mwangahakika.backend.dto.TransferRequest;
import com.mwangahakika.backend.dto.TransferResponse;
import com.mwangahakika.backend.entity.Transfer;
import com.mwangahakika.backend.entity.User;
import com.mwangahakika.backend.entity.Wallet;
import com.mwangahakika.backend.entity.WalletTransaction;
import com.mwangahakika.backend.enums.Role;
import com.mwangahakika.backend.enums.TransactionType;
import com.mwangahakika.backend.enums.UserStatus;
import com.mwangahakika.backend.enums.WalletStatus;
import com.mwangahakika.backend.exception.InsufficientBalanceException;
import com.mwangahakika.backend.exception.ResourceNotFoundException;
import com.mwangahakika.backend.exception.UnauthorizedActionException;
import com.mwangahakika.backend.repository.TransferRepository;
import com.mwangahakika.backend.repository.UserRepository;
import com.mwangahakika.backend.repository.WalletRepository;
import com.mwangahakika.backend.repository.WalletTransactionRepository;
import com.mwangahakika.backend.service.impl.TransferServiceImpl;

@ExtendWith(MockitoExtension.class)
class TransferServiceImplTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private TransferRepository transferRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TransferServiceImpl transferService;

    private User senderUser;
    private User receiverUser;
    private Wallet senderWallet;
    private Wallet receiverWallet;

    @BeforeEach
    void setUp() {
        senderUser = User.builder()
                .id(1L)
                .fullName("User One")
                .email("user1@demo.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        receiverUser = User.builder()
                .id(2L)
                .fullName("User Two")
                .email("user2@demo.com")
                .passwordHash("hash")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        senderWallet = Wallet.builder()
                .id(10L)
                .user(senderUser)
                .balance(new BigDecimal("5000.00"))
                .currency("TZS")
                .status(WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        receiverWallet = Wallet.builder()
                .id(20L)
                .user(receiverUser)
                .balance(new BigDecimal("1000.00"))
                .currency("TZS")
                .status(WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void transfer_shouldSucceed() {
        TransferRequest request = new TransferRequest(10L, 20L, new BigDecimal("1000.00"));

        when(walletRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(receiverWallet));
        when(userRepository.findById(1L)).thenReturn(Optional.of(senderUser));

        when(transferRepository.save(any(Transfer.class))).thenAnswer(invocation -> {
            Transfer t = invocation.getArgument(0);
            t.setId(100L);
            return t;
        });

        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TransferResponse response = transferService.transfer(1L, request);

        assertNotNull(response);
        assertEquals(100L, response.transferId());
        assertEquals("SUCCESS", response.status());
        assertEquals(new BigDecimal("1000.00"), response.amount());

        assertEquals(new BigDecimal("4000.00"), senderWallet.getBalance());
        assertEquals(new BigDecimal("2000.00"), receiverWallet.getBalance());

        verify(transferRepository).save(any(Transfer.class));
        verify(walletRepository, times(2)).save(any(Wallet.class));
        verify(walletTransactionRepository, times(2)).save(any(WalletTransaction.class));

        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository, times(2)).save(txCaptor.capture());

        assertTrue(txCaptor.getAllValues().stream()
                .anyMatch(tx -> tx.getTransactionType() == TransactionType.TRANSFER_OUT));
        assertTrue(txCaptor.getAllValues().stream()
                .anyMatch(tx -> tx.getTransactionType() == TransactionType.TRANSFER_IN));
    }

    @Test
    void transfer_shouldFail_whenInsufficientBalance() {
        TransferRequest request = new TransferRequest(10L, 20L, new BigDecimal("6000.00"));

        when(walletRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(receiverWallet));

        assertThrows(InsufficientBalanceException.class, () ->
                transferService.transfer(1L, request));

        verify(transferRepository, never()).save(any());
        verify(walletTransactionRepository, never()).save(any());
    }

    @Test
    void transfer_shouldFail_whenSenderDoesNotOwnWallet() {
        TransferRequest request = new TransferRequest(10L, 20L, new BigDecimal("1000.00"));

        when(walletRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(senderWallet));
        when(walletRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(receiverWallet));

        assertThrows(UnauthorizedActionException.class, () ->
                transferService.transfer(999L, request));

        verify(transferRepository, never()).save(any());
    }

    @Test
    void transfer_shouldFail_whenWalletNotFound() {
        TransferRequest request = new TransferRequest(10L, 20L, new BigDecimal("1000.00"));

        when(walletRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                transferService.transfer(1L, request));
    }

    @Test
    void transfer_shouldFail_whenSameWallet() {
        TransferRequest request = new TransferRequest(10L, 10L, new BigDecimal("100.00"));

        assertThrows(IllegalArgumentException.class, () ->
                transferService.transfer(1L, request));
    }
}
