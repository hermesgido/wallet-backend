package com.mwangahakika.backend;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.mwangahakika.backend.dto.AdminTopUpRequest;
import com.mwangahakika.backend.dto.AdminTopUpResponse;
import com.mwangahakika.backend.entity.User;
import com.mwangahakika.backend.entity.Wallet;
import com.mwangahakika.backend.entity.WalletTransaction;
import com.mwangahakika.backend.enums.Role;
import com.mwangahakika.backend.enums.TransactionType;
import com.mwangahakika.backend.enums.UserStatus;
import com.mwangahakika.backend.enums.WalletStatus;
import com.mwangahakika.backend.exception.ResourceNotFoundException;
import com.mwangahakika.backend.repository.UserRepository;
import com.mwangahakika.backend.repository.WalletRepository;
import com.mwangahakika.backend.repository.WalletTransactionRepository;
import com.mwangahakika.backend.service.impl.WalletServiceImpl;

@ExtendWith(MockitoExtension.class)
class WalletServiceImplTest {

    @Mock
    private WalletRepository walletRepository;
    @Mock
    private WalletTransactionRepository walletTransactionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WalletServiceImpl walletService;

    @Test
    void adminTopUp_shouldSucceed() {
        User admin = User.builder()
                .id(99L)
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        Wallet wallet = Wallet.builder()
                .id(10L)
                .balance(new BigDecimal("1000.00"))
                .currency("TZS")
                .status(WalletStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        AdminTopUpRequest request = new AdminTopUpRequest(new BigDecimal("500.00"), "Adjustment");

        when(walletRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(wallet));
        when(userRepository.findById(99L)).thenReturn(Optional.of(admin));
        when(walletTransactionRepository.save(any(WalletTransaction.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        AdminTopUpResponse response = walletService.adminTopUp(99L, 10L, request);

        assertEquals(10L, response.walletId());
        assertEquals(new BigDecimal("1000.00"), response.balanceBefore());
        assertEquals(new BigDecimal("1500.00"), response.balanceAfter());
        assertEquals(new BigDecimal("500.00"), response.amount());
        assertNotNull(response.reference());

        verify(walletRepository).save(wallet);
        verify(walletTransactionRepository).save(any(WalletTransaction.class));

        ArgumentCaptor<WalletTransaction> txCaptor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionRepository).save(txCaptor.capture());
        assertEquals(TransactionType.TOP_UP, txCaptor.getValue().getTransactionType());
    }

    @Test
    void adminTopUp_shouldFail_whenWalletMissing() {
        when(walletRepository.findByIdForUpdate(10L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () ->
                walletService.adminTopUp(99L, 10L, new AdminTopUpRequest(new BigDecimal("100.00"), null)));
    }

    @Test
    void adminTopUp_shouldFail_whenAmountInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
                walletService.adminTopUp(99L, 10L, new AdminTopUpRequest(BigDecimal.ZERO, null)));
    }
}
