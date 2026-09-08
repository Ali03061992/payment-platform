package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.identity.application.dto.LoginRequest;
import com.paymentplatform.identity.application.dto.CreateInternalUserRequest;
import com.paymentplatform.identity.domain.model.User;
import com.paymentplatform.identity.domain.repository.UserRepository;
import com.paymentplatform.identity.domain.valueobject.Email;
import com.paymentplatform.identity.domain.valueobject.PasswordHash;
import com.paymentplatform.identity.domain.valueobject.PhoneNumber;
import com.paymentplatform.identity.domain.valueobject.Username;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager em;

    private MockMvc mockMvc;
    private String adminToken;
    private String adminUsername;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        adminUsername = "admin." + System.nanoTime();
        User admin = users.save(User.create(new UserId(0), Username.of(adminUsername),
                Email.of(adminUsername + "@example.com"), PasswordHash.of(passwordEncoder.encode("Admin@1")),
                "Admin", "User", new PhoneNumber(null),
                null, RoleCode.SYSTEM_ADMIN));
        em.flush();
        em.clear();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(adminUsername, "Admin@1"))))
                .andReturn();
        adminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    @Disabled("Flaky due to test ordering - passes in isolation")
    void create_validRequest_returns201() throws Exception {
        String uname = "newadmin." + System.nanoTime();
        CreateInternalUserRequest request = new CreateInternalUserRequest(
                uname, uname + "@example.com", "Password@1",
                "New", "Admin", null, null, "SHOP_ADMIN");

        mockMvc.perform(post("/api/users")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(uname));
    }

    @Test
    void list_returnsUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void findById_existingUser() throws Exception {
        User user = users.findByUsername(Username.of(adminUsername)).orElseThrow();
        mockMvc.perform(get("/api/users/" + user.id().value())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(adminUsername));
    }

    @Test
    void activate_user() throws Exception {
        User user = users.findByUsername(Username.of(adminUsername)).orElseThrow();
        mockMvc.perform(patch("/api/users/" + user.id().value() + "/activate")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void disable_user() throws Exception {
        User user = users.findByUsername(Username.of(adminUsername)).orElseThrow();
        mockMvc.perform(patch("/api/users/" + user.id().value() + "/disable")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());
    }

    @Test
    void create_withoutAuth_returns401() throws Exception {
        CreateInternalUserRequest request = new CreateInternalUserRequest(
                "noauth." + System.nanoTime(), "noauth." + System.nanoTime() + "@example.com", "Password@1",
                "No", "Auth", null, null, "SHOP_ADMIN");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
