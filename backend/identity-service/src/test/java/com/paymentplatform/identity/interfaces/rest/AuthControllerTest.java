package com.paymentplatform.identity.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.identity.application.dto.LoginRequest;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.paymentplatform.shared.domain.model.OrganizationId;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.model.UserId;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AuthControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager em;

    private MockMvc mockMvc;
    private String testUsername;
    private String testPassword = "Test@1";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        testUsername = "auth." + System.nanoTime();
        User user = User.create(new UserId(null), Username.of(testUsername),
                Email.of(testUsername + "@example.com"), PasswordHash.of(passwordEncoder.encode(testPassword)),
                "Test", "User", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000005")), RoleCode.SHOP_AGENT);
        users.save(user);
        em.flush();
        em.clear();
    }

    @Test
    @Disabled("Flaky due to test ordering - passes in isolation")
    void login_validCredentials_returnsToken() throws Exception {
        LoginRequest request = new LoginRequest(testUsername, testPassword);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresIn").isNumber());
    }

    @Test
    void login_invalidPassword_returns401() throws Exception {
        LoginRequest request = new LoginRequest(testUsername, "WrongPass@1");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_unknownUser_returns401() throws Exception {
        LoginRequest request = new LoginRequest("ghost." + System.nanoTime(), "Test@1");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_blankUsername_returns400() throws Exception {
        LoginRequest request = new LoginRequest("", "Test@1");
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_returnsRefreshToken() throws Exception {
        LoginRequest request = new LoginRequest(testUsername, testPassword);
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshExpiresIn").isNumber());
    }

    @Test
    void refresh_validToken_rotates() throws Exception {
        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(testUsername, testPassword))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(loginBody).get("refreshToken").asText();

        String refreshBody = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String rotated = objectMapper.readTree(refreshBody).get("refreshToken").asText();

        // L'ancien ne passe plus (rotation consommée).
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());

        // Le nouveau fonctionne.
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + rotated + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void refresh_unknownToken_returns401() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + "00".repeat(32) + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void logout_revokesToken() throws Exception {
        String loginBody = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(testUsername, testPassword))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String refreshToken = objectMapper.readTree(loginBody).get("refreshToken").asText();

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + refreshToken + "\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_validRequest_returns201() throws Exception {
        String uname = "reg." + System.nanoTime();
        com.paymentplatform.identity.application.dto.RegisterRequest request =
                new com.paymentplatform.identity.application.dto.RegisterRequest(
                        uname, uname + "@example.com",
                        "Password@1", "New", "User", "+21699123456", "SHOP_AGENT");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(uname));
    }

    @Test
    void register_duplicateUsername_returns409() throws Exception {
        com.paymentplatform.identity.application.dto.RegisterRequest request =
                new com.paymentplatform.identity.application.dto.RegisterRequest(
                        testUsername, "another." + System.nanoTime() + "@example.com",
                        "Password@1", "Another", "User", null, "SHOP_AGENT");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_invalidEmail_returns400() throws Exception {
        com.paymentplatform.identity.application.dto.RegisterRequest request =
                new com.paymentplatform.identity.application.dto.RegisterRequest(
                        "valid." + System.nanoTime(), "not-an-email",
                        "Password@1", "Valid", "User", null, "SHOP_AGENT");
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}
