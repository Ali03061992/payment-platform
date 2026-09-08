package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.identity.application.dto.LoginRequest;
import com.paymentplatform.identity.application.dto.AgentRequest;
import com.paymentplatform.identity.application.dto.UpdateAgentRequest;
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
class SupplierAgentControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository users;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private EntityManager em;

    private MockMvc mockMvc;
    private String supplierAdminToken;
    private static final long SUPPLIER_ID = 42L;

    @BeforeEach
    void setUp() throws Exception {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        String supplierAdminUsername = "supplier.admin." + System.nanoTime();
        User supplierAdmin = users.save(User.create(new UserId(0), Username.of(supplierAdminUsername),
                Email.of(supplierAdminUsername + "@example.com"), PasswordHash.of(passwordEncoder.encode("Admin@1")),
                "Supplier", "Admin", new PhoneNumber(null),
                OrganizationId.of(SUPPLIER_ID), RoleCode.SUPPLIER_ADMIN));
        em.flush();
        em.clear();

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(supplierAdminUsername, "Admin@1"))))
                .andReturn();
        supplierAdminToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken").asText();
    }

    @Test
    void create_validAgent_returns201() throws Exception {
        AgentRequest request = new AgentRequest("Agent", "Test", "agent." + System.nanoTime() + "@example.com",
                "55123456", "agent." + System.nanoTime(), "SUPPLIER_AGENT", "Agent@123");

        mockMvc.perform(post("/api/suppliers/" + SUPPLIER_ID + "/agents")
                        .header("Authorization", "Bearer " + supplierAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.username").exists());
    }

    @Test
    void list_returnsAgents() throws Exception {
        mockMvc.perform(get("/api/suppliers/" + SUPPLIER_ID + "/agents")
                        .header("Authorization", "Bearer " + supplierAdminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private long createAgent(String suffix) throws Exception {
        AgentRequest createReq = new AgentRequest("Agent", "Test" + suffix, "agent." + suffix + "@example.com",
                null, "agent." + suffix, "SUPPLIER_AGENT", "Agent@123");
        MvcResult result = mockMvc.perform(post("/api/suppliers/" + SUPPLIER_ID + "/agents")
                        .header("Authorization", "Bearer " + supplierAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("user").get("id").asLong();
    }

    @Test
    void update_agent() throws Exception {
        long agentId = createAgent("update" + System.nanoTime());

        UpdateAgentRequest updateReq = new UpdateAgentRequest("Updated", "Agent", "updated." + System.nanoTime() + "@example.com", "99887766");
        mockMvc.perform(patch("/api/suppliers/" + SUPPLIER_ID + "/agents/" + agentId)
                        .header("Authorization", "Bearer " + supplierAdminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Updated"));
    }

    @Test
    void activate_agent() throws Exception {
        long agentId = createAgent("activate" + System.nanoTime());

        mockMvc.perform(patch("/api/suppliers/" + SUPPLIER_ID + "/agents/" + agentId + "/activate")
                        .header("Authorization", "Bearer " + supplierAdminToken))
                .andExpect(status().isOk());
    }

    @Test
    void disable_agent() throws Exception {
        long agentId = createAgent("disable" + System.nanoTime());

        mockMvc.perform(patch("/api/suppliers/" + SUPPLIER_ID + "/agents/" + agentId + "/disable")
                        .header("Authorization", "Bearer " + supplierAdminToken))
                .andExpect(status().isOk());
    }
}
