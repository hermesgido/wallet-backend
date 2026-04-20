package com.mwangahakika.backend.config;


import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.mwangahakika.backend.entity.User;
import com.mwangahakika.backend.entity.Wallet;
import com.mwangahakika.backend.enums.Role;
import com.mwangahakika.backend.enums.UserStatus;
import com.mwangahakika.backend.enums.WalletStatus;
import com.mwangahakika.backend.repository.UserRepository;
import com.mwangahakika.backend.repository.WalletRepository;

import lombok.RequiredArgsConstructor;

@Component
@Profile("dev")
@RequiredArgsConstructor
public class DevDataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        if (userRepository.existsByEmail("admin@demo.com")) {
            return; 
        }

        LocalDateTime now = LocalDateTime.now();
        String password = passwordEncoder.encode("Password@123");

        User admin = userRepository.save(User.builder()
                .fullName("System Admin")
                .email("admin@demo.com")
                .passwordHash(password)
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        User user1 = userRepository.save(User.builder()
                .fullName("User One")
                .email("user1@demo.com")
                .passwordHash(password)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        User user2 = userRepository.save(User.builder()
                .fullName("User Two")
                .email("user2@demo.com")
                .passwordHash(password)
                .role(Role.USER)
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        walletRepository.save(Wallet.builder()
                .user(admin)
                .balance(BigDecimal.ZERO)
                .currency("TZS")
                .status(WalletStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        walletRepository.save(Wallet.builder()
                .user(user1)
                .balance(new BigDecimal("50000.00"))
                .currency("TZS")
                .status(WalletStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        walletRepository.save(Wallet.builder()
                .user(user2)
                .balance(new BigDecimal("10000.00"))
                .currency("TZS")
                .status(WalletStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }
}
