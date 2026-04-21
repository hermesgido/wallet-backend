package com.mwangahakika.backend;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mwangahakika.backend.dto.CreateTopUpRequestRequest;
import com.mwangahakika.backend.dto.ReviewTopUpRequestRequest;
import com.mwangahakika.backend.dto.TopUpRequestResponse;
import com.mwangahakika.backend.entity.TopUpRequest;
import com.mwangahakika.backend.entity.User;
import com.mwangahakika.backend.entity.Wallet;
import com.mwangahakika.backend.entity.WalletTransaction;
import com.mwangahakika.backend.enums.Role;
import com.mwangahakika.backend.enums.TopUpRequestStatus;
import com.mwangahakika.backend.enums.UserStatus;
import com.mwangahakika.backend.enums.WalletStatus;
import com.mwangahakika.backend.exception.ResourceNotFoundException;
import com.mwangahakika.backend.exception.UnauthorizedActionException;
import com.mwangahakika.backend.repository.TopUpRequestRepository;
import com.mwangahakika.backend.repository.UserRepository;
import com.mwangahakika.backend.repository.WalletRepository;
import com.mwangahakika.backend.repository.WalletTransactionRepository;
import com.mwangahakika.backend.service.impl.TopUpRequestServiceImpl;

@ExtendWith(MockitoExtension.class)
class TopUpRequestServiceImplTest {

    @Mock
    private TopUpRequestRepository topUpRequestRepository;
    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private TopUpRequestServiceImpl service;

    private User user;
    private User admin;
    private Wallet wallet;
    private TopUpRequest pendingRequest;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("user1@demo.com")
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .build();

        admin = User.builder()
                .id(99L)
                .email("admin@demo.com")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        wallet = Wallet.builder()
                .id(10L)
                .user(user)
                .balance(new BigDecimal("1000.00"))
                .currency("TZS")
                .status(WalletStatus.ACTIVE)
                .build();

        pendingRequest = TopUpRequest.builder()
                .id(50L)
                .user(user)
                .wallet(wallet)
                .amount(new BigDecimal("500.00"))
                .status(TopUpRequestStatus.PENDING)
                .requestedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    @Test
    void create_shouldSucceed() {
        CreateTopUpRequestRequest request = new CreateTopUpRequestRequest(
                10L, new BigDecimal("500.00"), "Need funds"
        );

        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(topUpRequestRepository.save(any(TopUpRequest.class))).thenAnswer(invocation -> {
            TopUpRequest entity = invocation.getArgument(0);
            entity.setId(50L);
            return entity;
        });

        TopUpRequestResponse response = service.create(1L, request);

        assertNotNull(response);
        assertEquals(50L, response.id());
        assertEquals("PENDING", response.status());
        assertEquals(new BigDecimal("500.00"), response.amount());
    }

    @Test
    void create_shouldFail_whenWalletDoesNotBelongToUser() {
        CreateTopUpRequestRequest request = new CreateTopUpRequestRequest(
                10L, new BigDecimal("500.00"), "Need funds"
        );

        when(walletRepository.findById(10L)).thenReturn(Optional.of(wallet));

        assertThrows(UnauthorizedActionException.class, () ->
                service.create(999L, request));
    }

    @Test
    void approve_shouldSucceed() {
        ReviewTopUpRequestRequest review = new ReviewTopUpRequestRequest("Approved");

        when(topUpRequestRepository.findById(50L)).thenReturn(Optional.of(pendingRequest));
        when(walletRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(wallet));
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(topUpRequestRepository.save(any(TopUpRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TopUpRequestResponse response = service.approve(99L, 50L, review);

        assertEquals("APPROVED", response.status());
        assertEquals(new BigDecimal("1500.00"), wallet.getBalance());
        assertEquals(99L, response.reviewedBy());

        verify(walletTransactionRepository).save(any(WalletTransaction.class));
    }

    @Test
    void approve_shouldFail_whenAlreadyProcessed() {
        pendingRequest.setStatus(TopUpRequestStatus.APPROVED);

        when(topUpRequestRepository.findById(50L)).thenReturn(Optional.of(pendingRequest));

        assertThrows(IllegalStateException.class, () ->
                service.approve(99L, 50L, new ReviewTopUpRequestRequest("Again")));
    }

    @Test
    void reject_shouldSucceed() {
        ReviewTopUpRequestRequest review = new ReviewTopUpRequestRequest("Rejected");

        when(topUpRequestRepository.findById(50L)).thenReturn(Optional.of(pendingRequest));
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(topUpRequestRepository.save(any(TopUpRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TopUpRequestResponse response = service.reject(99L, 50L, review);

        assertEquals("REJECTED", response.status());
        assertEquals(99L, response.reviewedBy());
        verify(walletTransactionRepository, never()).save(any());
    }

    @Test
    void approve_shouldFail_whenRequestMissing() {
        when(topUpRequestRepository.findById(50L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                service.approve(99L, 50L, new ReviewTopUpRequestRequest("Approved")));
    }
}
