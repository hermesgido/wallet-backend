package com.mwangahakika.backend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.mwangahakika.backend.dto.CreateTopUpRequestRequest;
import com.mwangahakika.backend.dto.ReviewTopUpRequestRequest;
import com.mwangahakika.backend.dto.TransferRequest;
import com.mwangahakika.backend.entity.TopUpRequest;
import com.mwangahakika.backend.enums.Role;

class FlowTest extends BaseIt {

    @BeforeEach
    void setUp() {
        resetDb();
    }

    @Test
    void transfer() throws Exception {
        var sender = user("Sender", "sender@example.com", Role.USER);
        var senderWallet = wallet(sender, "5000.00");
        var receiver = user("Receiver", "receiver@example.com", Role.USER);
        var receiverWallet = wallet(receiver, "1000.00");
        var token = token(sender.getEmail());

        mvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new TransferRequest(senderWallet.getId(), receiverWallet.getId(), new BigDecimal("1000.00")))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"));

        assertEquals(new BigDecimal("4000.00"), wallets.findById(senderWallet.getId()).orElseThrow().getBalance());
        assertEquals(new BigDecimal("2000.00"), wallets.findById(receiverWallet.getId()).orElseThrow().getBalance());
    }

    @Test
    void topUpRequest() throws Exception {
        var admin = user("Admin", "admin@example.com", Role.ADMIN);
        wallet(admin, "0.00");
        var user = user("User", "user@example.com", Role.USER);
        var wallet = wallet(user, "500.00");
        var userToken = token(user.getEmail());
        var adminToken = token(admin.getEmail());

        mvc.perform(post("/api/top-up-requests")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new CreateTopUpRequestRequest(wallet.getId(), new BigDecimal("300.00"), "Need funds"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        TopUpRequest request = topUps.findAll().getFirst();

        mvc.perform(post("/api/admin/top-up-requests/{id}/approve", request.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new ReviewTopUpRequestRequest("Approved"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        assertEquals(new BigDecimal("800.00"), wallets.findById(wallet.getId()).orElseThrow().getBalance());
    }

    @Test
    void transferNoBalance() throws Exception {
        var sender = user("Sender", "sender@example.com", Role.USER);
        var senderWallet = wallet(sender, "100.00");
        var receiver = user("Receiver", "receiver@example.com", Role.USER);
        var receiverWallet = wallet(receiver, "1000.00");
        var token = token(sender.getEmail());

        mvc.perform(post("/api/transfers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new TransferRequest(senderWallet.getId(), receiverWallet.getId(), new BigDecimal("500.00")))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient wallet balance."));
    }
}
