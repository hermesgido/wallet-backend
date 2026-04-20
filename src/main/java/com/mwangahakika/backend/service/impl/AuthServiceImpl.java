package com.mwangahakika.backend.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.mwangahakika.backend.dto.AuthResponse;
import com.mwangahakika.backend.dto.LoginRequest;
import com.mwangahakika.backend.dto.RegisterRequest;
import com.mwangahakika.backend.dto.RegisterResponse;
import com.mwangahakika.backend.entity.User;
import com.mwangahakika.backend.entity.Wallet;
import com.mwangahakika.backend.enums.Role;
import com.mwangahakika.backend.enums.UserStatus;
import com.mwangahakika.backend.enums.WalletStatus;
import com.mwangahakika.backend.repository.UserRepository;
import com.mwangahakika.backend.repository.WalletRepository;
import com.mwangahakika.backend.security.AuthenticatedUser;
import com.mwangahakika.backend.security.JwtService;
import com.mwangahakika.backend.service.AuthService;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final WalletRepository walletRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email(),
                        request.password()
                )
        );

        AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
        String token = jwtService.generateToken(user);

        return new AuthResponse(token);
    }

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email already exists");
        }

        LocalDateTime now = LocalDateTime.now();

        User user = User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(Role.USER) 
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        User savedUser = userRepository.save(user);

        Wallet wallet = Wallet.builder()
                .user(savedUser)
                .balance(BigDecimal.ZERO)
                .currency("TZS")
                .status(WalletStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Wallet savedWallet = walletRepository.save(wallet);

        return new RegisterResponse(
                savedUser.getId(),
                savedUser.getEmail(),
                savedWallet.getId()
        );
    }
}
