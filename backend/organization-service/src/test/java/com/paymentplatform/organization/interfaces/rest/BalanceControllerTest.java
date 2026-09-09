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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BalanceControllerTest {

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

    private UsernamePasswordAuthenticationToken systemAdmin() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000001"), "admin", List.of("SYSTEM_ADMIN"), null);
    }

    private UsernamePasswordAuthenticationToken supplierAdmin() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000002"), "supplier.admin", List.of("SUPPLIER_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
    }

    @Test
    void listSupplierBalances_asSystemAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/api/balances/supplier/00000000-0000-0000-0000-000000000010")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(systemAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listSupplierBalances_asOwner_returnsOk() throws Exception {
        mockMvc.perform(get("/api/balances/supplier/00000000-0000-0000-0000-000000000010")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listSupplierBalances_asOtherSupplier_returns403() throws Exception {
        UsernamePasswordAuthenticationToken otherSupplier = auth(UUID.fromString("00000000-0000-0000-0000-000000000003"), "other", List.of("SUPPLIER_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000099"));

        mockMvc.perform(get("/api/balances/supplier/00000000-0000-0000-0000-000000000010")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(otherSupplier)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adjustBalance_asSystemAdmin_returnsOk() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.BalanceController.AdjustBalanceRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), new BigDecimal("50.00"), "Test adjustment"));

        mockMvc.perform(post("/api/balances/adjust")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(systemAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    @Test
    void adjustBalance_asNonAdmin_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.BalanceController.AdjustBalanceRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), new BigDecimal("50.00"), "Test"));

        mockMvc.perform(post("/api/balances/adjust")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void listShopBalances_asSystemAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/api/balances/shop/00000000-0000-0000-0000-000000000020")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(systemAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getBalanceHistory_asSystemAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/api/balances/supplier/00000000-0000-0000-0000-000000000010/shop/00000000-0000-0000-0000-000000000020")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(systemAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
