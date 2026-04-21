package com.mwangahakika.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.mwangahakika.backend.dto.AdminTopUpRequest;
import com.mwangahakika.backend.enums.Role;

class WalletTest extends BaseIt {

    @BeforeEach
    void setUp() {
        resetDb();
    }

    @Test
    void adminTopUp() throws Exception {
        var admin = user("Admin", "admin@example.com", Role.ADMIN);
        wallet(admin, "0.00");
        var user = user("User", "user@example.com", Role.USER);
        var wallet = wallet(user, "500.00");
        var token = token(admin.getEmail());

        mvc.perform(post("/api/admin/wallets/{id}/top-up", wallet.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "top-up-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AdminTopUpRequest(new BigDecimal("250.00"), "Test"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(wallet.getId()))
                .andExpect(jsonPath("$.balanceAfter").value(750.00));

        assertEquals(new BigDecimal("750.00"), wallets.findById(wallet.getId()).orElseThrow().getBalance());
    }

    @Test
    void userBlockedFromAdminTopUp() throws Exception {
        var actor = user("User", "user@example.com", Role.USER);
        wallet(actor, "10.00");
        var target = user("Target", "target@example.com", Role.USER);
        var wallet = wallet(target, "500.00");
        var token = token(actor.getEmail());

        mvc.perform(post("/api/admin/wallets/{id}/top-up", wallet.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "top-up-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AdminTopUpRequest(new BigDecimal("50.00"), null))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void adminTopUpIsIdempotent() throws Exception {
        var admin = user("Admin", "admin@example.com", Role.ADMIN);
        wallet(admin, "0.00");
        var user = user("User", "user@example.com", Role.USER);
        var wallet = wallet(user, "500.00");
        var token = token(admin.getEmail());
        var request = json.writeValueAsString(new AdminTopUpRequest(new BigDecimal("250.00"), "Test"));

        mvc.perform(post("/api/admin/wallets/{id}/top-up", wallet.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "top-up-replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter").value(750.00));

        mvc.perform(post("/api/admin/wallets/{id}/top-up", wallet.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "top-up-replay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balanceAfter").value(750.00));

        assertEquals(new BigDecimal("750.00"), wallets.findById(wallet.getId()).orElseThrow().getBalance());
        assertEquals(1, txs.count());
    }

    @Test
    void adminTopUpIdempotencyConflict() throws Exception {
        var admin = user("Admin", "admin@example.com", Role.ADMIN);
        wallet(admin, "0.00");
        var user = user("User", "user@example.com", Role.USER);
        var wallet = wallet(user, "500.00");
        var token = token(admin.getEmail());

        mvc.perform(post("/api/admin/wallets/{id}/top-up", wallet.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "top-up-conflict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AdminTopUpRequest(new BigDecimal("250.00"), "Test"))))
                .andExpect(status().isOk());

        mvc.perform(post("/api/admin/wallets/{id}/top-up", wallet.getId())
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", "top-up-conflict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new AdminTopUpRequest(new BigDecimal("300.00"), "Different"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Idempotency key has already been used with a different request."));
    }
}
