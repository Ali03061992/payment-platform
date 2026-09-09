package com.paymentplatform.organization.interfaces.rest;

import java.util.UUID;

import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminOrganizationControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private static UsernamePasswordAuthenticationToken auth(UUID userId, String username, List<String> perms, UUID orgId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, username, perms, orgId);
        var authorities = perms.stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private UsernamePasswordAuthenticationToken adminUser() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000001"), "admin", List.of("ADMIN_MANAGE_ORGANIZATIONS", "ADMIN_VIEW_STATS"), null);
    }

    @Test
    void createSupplier_validRequest_returns201() throws Exception {
        String name = "Supplier-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(new com.paymentplatform.organization.application.dto.CreateOrganizationRequest(name, "SUPPLIER"));

        mockMvc.perform(post("/api/admin/suppliers")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("SUPPLIER"));
    }

    @Test
    void listSuppliers_returnsList() throws Exception {
        mockMvc.perform(get("/api/admin/suppliers")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createShop_validRequest_returns201() throws Exception {
        String name = "Shop-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(new com.paymentplatform.organization.application.dto.CreateOrganizationRequest(name, "SHOP"));

        mockMvc.perform(post("/api/admin/shops")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("SHOP"));
    }

    @Test
    void listShops_returnsList() throws Exception {
        mockMvc.perform(get("/api/admin/shops")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void stats_returnsStats() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suppliers").isNumber())
                .andExpect(jsonPath("$.shops").isNumber());
    }

    @Test
    void createSupplier_withoutAuth_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(new com.paymentplatform.organization.application.dto.CreateOrganizationRequest("Test", "SUPPLIER"));

        mockMvc.perform(post("/api/admin/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listSuppliers_withWrongRole_returns403() throws Exception {
        UsernamePasswordAuthenticationToken shopUser = auth(UUID.fromString("00000000-0000-0000-0000-000000000002"), "shop.user", List.of("SHOP_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000010"));

        mockMvc.perform(get("/api/admin/suppliers")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser)))
                .andExpect(status().isForbidden());
    }
}
