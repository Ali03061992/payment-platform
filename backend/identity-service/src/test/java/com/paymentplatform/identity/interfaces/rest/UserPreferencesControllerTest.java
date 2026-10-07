package com.paymentplatform.identity.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.identity.application.dto.LoginRequest;
import com.paymentplatform.identity.application.dto.UpdatePreferencesRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserPreferencesControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager em;

    private MockMvc mockMvc;
    private String token;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        String username = "prefs." + System.nanoTime();
        users.save(User.create(new UserId(null), Username.of(username),
                Email.of(username + "@example.com"), PasswordHash.of(passwordEncoder.encode("Test@1")),
                "Prefs", "User", new PhoneNumber(null),
                OrganizationId.of(UUID.fromString("00000000-0000-0000-0000-000000000005")), RoleCode.SHOP_AGENT));
        em.flush();
        em.clear();

        var loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(username, "Test@1"))))
                .andReturn();
        token = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    void updatePreferences_fullUpdate_returns200() throws Exception {
        var request = new UpdatePreferencesRequest("en", "#ff0000", "#00ff00", true);
        mockMvc.perform(patch("/api/users/me/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.preferredLang").value("en"))
                .andExpect(jsonPath("$.accentColor1").value("#ff0000"))
                .andExpect(jsonPath("$.accentColor2").value("#00ff00"))
                .andExpect(jsonPath("$.tourSeen").value(true));
    }

    @Test
    void updatePreferences_invalidLang_returns400() throws Exception {
        mockMvc.perform(patch("/api/users/me/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"preferredLang\":\"de\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePreferences_invalidColor_returns400() throws Exception {
        mockMvc.perform(patch("/api/users/me/preferences")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accentColor1\":\"red\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatePreferences_withoutAuth_returns401() throws Exception {
        var request = new UpdatePreferencesRequest("en", null, null, null);
        mockMvc.perform(patch("/api/users/me/preferences")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
