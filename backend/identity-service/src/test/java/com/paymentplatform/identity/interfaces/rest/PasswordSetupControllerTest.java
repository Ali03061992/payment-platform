package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.identity.infrastructure.email.PasswordSetupToken;
import com.paymentplatform.identity.infrastructure.email.PasswordSetupTokenRepository;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class PasswordSetupControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private PasswordSetupTokenRepository tokenRepository;

    private MockMvc mockMvc;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
        testUser = users.save(User.create(new UserId(0), Username.of("pwd.setup." + System.nanoTime()),
                Email.of("pwd.setup." + System.nanoTime() + "@example.com"), PasswordHash.of(passwordEncoder.encode("Test@1")),
                "Test", "User", new PhoneNumber(null),
                OrganizationId.of(5), RoleCode.SHOP_AGENT));
    }

    @Test
    void validateToken_validToken_returnsTrue() throws Exception {
        tokenRepository.save(
                new PasswordSetupToken(testUser.id().value(), "valid-token-123", Instant.now().plus(24, ChronoUnit.HOURS)));

        mockMvc.perform(get("/api/auth/password-setup/validate")
                        .param("token", "valid-token-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true));
    }

    @Test
    void validateToken_invalidToken_returnsFalse() throws Exception {
        mockMvc.perform(get("/api/auth/password-setup/validate")
                        .param("token", "nonexistent-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void complete_validRequest_setsPassword() throws Exception {
        tokenRepository.save(
                new PasswordSetupToken(testUser.id().value(), "setup-token-456", Instant.now().plus(24, ChronoUnit.HOURS)));

        Map<String, String> request = Map.of("token", "setup-token-456", "newPassword", "NewPass@123");
        mockMvc.perform(post("/api/auth/password-setup/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());
    }
}
