package com.mwangahakika.backend;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.mwangahakika.backend.dto.LoginRequest;
import com.mwangahakika.backend.dto.RegisterRequest;
import com.mwangahakika.backend.enums.Role;

class AuthTest extends BaseIt {

    @BeforeEach
    void setUp() {
        resetDb();
    }

    @Test
    void register() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegisterRequest("Jane", "jane@example.com", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.walletId").isNumber())
                .andExpect(jsonPath("$.email").value("jane@example.com"));
    }

    @Test
    void login() throws Exception {
        wallet(user("Jane", "jane@example.com", Role.USER), "100.00");

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest("jane@example.com", PASSWORD))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", not(blankOrNullString())));
    }

    @Test
    void authRequired() throws Exception {
        mvc.perform(get("/api/top-up-requests/my"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void loginRateLimit() throws Exception {
        wallet(user("Jane", "jane@example.com", Role.USER), "1000.00");

        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json.writeValueAsString(new LoginRequest("jane@example.com", "wrong-password"))))
                    .andExpect(status().isUnauthorized());
        }

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest("jane@example.com", "wrong-password"))))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many login attempts. Please try again later."));
    }
}
