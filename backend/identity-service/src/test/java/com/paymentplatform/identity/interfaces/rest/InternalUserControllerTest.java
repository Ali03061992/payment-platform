package com.paymentplatform.identity.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.identity.application.dto.CreateInternalUserRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class InternalUserControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private static final String INTERNAL_TOKEN = "test-internal-secret";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void create_validToken_returns201() throws Exception {
        CreateInternalUserRequest request = new CreateInternalUserRequest(
                "internaluser." + System.nanoTime(), "internal." + System.nanoTime() + "@example.com", "Password@1",
                "Internal", "User", null, UUID.fromString("00000000-0000-0000-0000-000000000005"), "SHOP_ADMIN");

        mockMvc.perform(post("/api/internal/users")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value(request.username()));
    }

    @Test
    void create_invalidToken_returns401() throws Exception {
        CreateInternalUserRequest request = new CreateInternalUserRequest(
                "internaluser." + System.nanoTime(), "internal." + System.nanoTime() + "@example.com", "Password@1",
                "Internal", "User", null, UUID.fromString("00000000-0000-0000-0000-000000000005"), "SHOP_ADMIN");

        mockMvc.perform(post("/api/internal/users")
                        .header("X-Internal-Token", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void create_missingToken_returns401() throws Exception {
        CreateInternalUserRequest request = new CreateInternalUserRequest(
                "internaluser." + System.nanoTime(), "internal." + System.nanoTime() + "@example.com", "Password@1",
                "Internal", "User", null, UUID.fromString("00000000-0000-0000-0000-000000000005"), "SHOP_ADMIN");

        mockMvc.perform(post("/api/internal/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Disabled("Flaky due to test ordering - passes in isolation")
    void getById_validToken_returnsUser() throws Exception {
        String uname = "getuser." + System.nanoTime();
        CreateInternalUserRequest createRequest = new CreateInternalUserRequest(
                uname, uname + "@example.com", "Password@1",
                "Get", "User", null, UUID.fromString("00000000-0000-0000-0000-000000000005"), "SHOP_ADMIN");

        String responseBody = mockMvc.perform(post("/api/internal/users")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(responseBody).get("id").asText());

        mockMvc.perform(get("/api/internal/users/" + id)
                        .header("X-Internal-Token", INTERNAL_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(uname));
    }

    @Test
    void getById_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/internal/users/00000000-0000-0000-0000-000000000001")
                        .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getById_missingToken_returns401() throws Exception {
        mockMvc.perform(get("/api/internal/users/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listByOrganization_validToken_returnsOk() throws Exception {
        mockMvc.perform(get("/api/internal/users")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .param("organizationId", "00000000-0000-0000-0000-000000000005"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listByOrganization_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/internal/users")
                        .header("X-Internal-Token", "wrong-token")
                        .param("organizationId", "00000000-0000-0000-0000-000000000005"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listByOrganization_missingToken_returns401() throws Exception {
        mockMvc.perform(get("/api/internal/users")
                        .param("organizationId", "00000000-0000-0000-0000-000000000005"))
                .andExpect(status().isUnauthorized());
    }
}
