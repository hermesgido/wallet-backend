package com.mwangahakika.backend;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mwangahakika.backend.dto.LoginRequest;
import com.mwangahakika.backend.entity.User;
import com.mwangahakika.backend.entity.Wallet;
import com.mwangahakika.backend.enums.Role;
import com.mwangahakika.backend.enums.UserStatus;
import com.mwangahakika.backend.enums.WalletStatus;
import com.mwangahakika.backend.repository.TopUpRequestRepository;
import com.mwangahakika.backend.repository.TransferRepository;
import com.mwangahakika.backend.repository.UserRepository;
import com.mwangahakika.backend.repository.WalletRepository;
import com.mwangahakika.backend.repository.WalletTransactionRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
abstract class BaseIt {

    protected static final String PASSWORD = "Password@123";

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    @Autowired
    protected UserRepository users;

    @Autowired
    protected WalletRepository wallets;

    @Autowired
    protected TransferRepository transfers;

    @Autowired
    protected TopUpRequestRepository topUps;

    @Autowired
    protected WalletTransactionRepository txs;

    @Autowired
    protected PasswordEncoder encoder;

    protected void resetDb() {
        txs.deleteAll();
        transfers.deleteAll();
        topUps.deleteAll();
        wallets.deleteAll();
        users.deleteAll();
    }

    protected User user(String name, String email, Role role) {
        LocalDateTime now = LocalDateTime.now();
        return users.save(User.builder()
                .fullName(name)
                .email(email)
                .passwordHash(encoder.encode(PASSWORD))
                .role(role)
                .status(UserStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    protected Wallet wallet(User user, String balance) {
        LocalDateTime now = LocalDateTime.now();
        return wallets.save(Wallet.builder()
                .user(user)
                .balance(new BigDecimal(balance))
                .currency("TZS")
                .status(WalletStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    protected String token(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        return json.readTree(body).get("token").asText();
    }
}
