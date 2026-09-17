package com.paymentplatform.organization.interfaces.rest;

import java.util.UUID;

import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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
class StockOptimizationControllerTest {

    @Autowired private WebApplicationContext wac;

    private MockMvc mockMvc;
    private static final UUID SUPPLIER_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

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

    private UsernamePasswordAuthenticationToken supplierAdmin() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000001"), "supplier.admin", List.of("SUPPLIER_ADMIN"), SUPPLIER_ID);
    }

    private UsernamePasswordAuthenticationToken supplierAgent() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000002"), "supplier.agent", List.of("SUPPLIER_AGENT"), SUPPLIER_ID);
    }

    private UsernamePasswordAuthenticationToken systemAdmin() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000003"), "sysadmin", List.of("SYSTEM_ADMIN"), null);
    }

    @Test
    void optimize_asSupplierAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/api/suppliers/" + SUPPLIER_ID + "/optimization")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray())
                .andExpect(jsonPath("$.summary").isNotEmpty());
    }

    @Test
    void optimize_asSupplierAgent_returnsOk() throws Exception {
        mockMvc.perform(get("/api/suppliers/" + SUPPLIER_ID + "/optimization")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAgent())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray());
    }

    @Test
    void optimize_asSystemAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/api/suppliers/" + SUPPLIER_ID + "/optimization")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(systemAdmin())))
                .andExpect(status().isOk());
    }

    @Test
    void optimize_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/suppliers/" + SUPPLIER_ID + "/optimization"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void optimize_withShopRole_returns403() throws Exception {
        var shopUser = auth(UUID.fromString("00000000-0000-0000-0000-000000000004"), "shop.user", List.of("SHOP_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000020"));
        mockMvc.perform(get("/api/suppliers/" + SUPPLIER_ID + "/optimization")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void configure_asSupplierAdmin_returnsOk() throws Exception {
        mockMvc.perform(post("/api/suppliers/" + SUPPLIER_ID + "/optimization/configure")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .param("leadTimeDays", "10")
                        .param("orderingCost", "60")
                        .param("holdingCostPercent", "0.3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.products").isArray());
    }

    @Test
    void configure_asSystemAdmin_returnsOk() throws Exception {
        mockMvc.perform(post("/api/suppliers/" + SUPPLIER_ID + "/optimization/configure")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(systemAdmin()))
                        .param("leadTimeDays", "5")
                        .param("orderingCost", "40")
                        .param("holdingCostPercent", "0.2"))
                .andExpect(status().isOk());
    }

    @Test
    void configure_withDefaultParams_returnsOk() throws Exception {
        mockMvc.perform(post("/api/suppliers/" + SUPPLIER_ID + "/optimization/configure")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isOk());
    }

    @Test
    void configure_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/suppliers/" + SUPPLIER_ID + "/optimization/configure"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void configure_withSupplierAgent_returns403() throws Exception {
        mockMvc.perform(post("/api/suppliers/" + SUPPLIER_ID + "/optimization/configure")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAgent())))
                .andExpect(status().isForbidden());
    }
}
