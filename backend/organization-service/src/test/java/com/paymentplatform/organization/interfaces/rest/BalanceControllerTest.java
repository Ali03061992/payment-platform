package com.paymentplatform.organization.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
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
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

    @Test
    void listShopBalances_asShopOwner_returnsOk() throws Exception {
        UsernamePasswordAuthenticationToken shopAdmin = auth(UUID.fromString("00000000-0000-0000-0000-000000000010"), "shop.admin", List.of("SHOP_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000020"));
        mockMvc.perform(get("/api/balances/shop/00000000-0000-0000-0000-000000000020")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin)))
                .andExpect(status().isOk());
    }

    @Test
    void listShopBalances_asOtherShop_returns403() throws Exception {
        UsernamePasswordAuthenticationToken otherShop = auth(UUID.fromString("00000000-0000-0000-0000-000000000011"), "other.shop", List.of("SHOP_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000099"));
        mockMvc.perform(get("/api/balances/shop/00000000-0000-0000-0000-000000000020")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(otherShop)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listShopBalances_asNonAdmin_returns403() throws Exception {
        UsernamePasswordAuthenticationToken supplierUser = auth(UUID.fromString("00000000-0000-0000-0000-000000000012"), "supplier.user", List.of("SUPPLIER_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        mockMvc.perform(get("/api/balances/shop/00000000-0000-0000-0000-000000000020")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBalanceHistory_asSupplierOwner_returnsOk() throws Exception {
        UsernamePasswordAuthenticationToken supplierOwner = auth(UUID.fromString("00000000-0000-0000-0000-000000000013"), "supplier.owner", List.of("SUPPLIER_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        mockMvc.perform(get("/api/balances/supplier/00000000-0000-0000-0000-000000000010/shop/00000000-0000-0000-0000-000000000020")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierOwner)))
                .andExpect(status().isOk());
    }

    @Test
    void getBalanceHistory_asShopOwner_returnsOk() throws Exception {
        UsernamePasswordAuthenticationToken shopOwner = auth(UUID.fromString("00000000-0000-0000-0000-000000000014"), "shop.owner", List.of("SHOP_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000020"));
        mockMvc.perform(get("/api/balances/supplier/00000000-0000-0000-0000-000000000010/shop/00000000-0000-0000-0000-000000000020")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopOwner)))
                .andExpect(status().isOk());
    }

    @Test
    void getBalanceHistory_asOtherOrg_returns403() throws Exception {
        UsernamePasswordAuthenticationToken other = auth(UUID.fromString("00000000-0000-0000-0000-000000000015"), "other.user", List.of("SUPPLIER_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000099"));
        mockMvc.perform(get("/api/balances/supplier/00000000-0000-0000-0000-000000000010/shop/00000000-0000-0000-0000-000000000020")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getBalanceHistory_withNullOrganizationId_returns403() throws Exception {
        UsernamePasswordAuthenticationToken noOrg = auth(UUID.fromString("00000000-0000-0000-0000-000000000016"), "no.org", List.of("SUPPLIER_ADMIN"), null);
        // SYSTEM_ADMIN is not present, and orgId is null -> should be 403
        // But PreAuthorize requires SUPPLIER_ADMIN, so it will pass auth but controller will check orgId null
        mockMvc.perform(get("/api/balances/supplier/00000000-0000-0000-0000-000000000010/shop/00000000-0000-0000-0000-000000000020")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(noOrg)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adjustBalance_withNullAmount_returns400() throws Exception {
        String body = "{\"supplierId\":\"00000000-0000-0000-0000-000000000010\",\"shopId\":\"00000000-0000-0000-0000-000000000020\",\"amount\":null,\"reason\":\"test\"}";
        mockMvc.perform(post("/api/balances/adjust")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(systemAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listSupplierBalances_asShopAdmin_returns403() throws Exception {
        UsernamePasswordAuthenticationToken shopAdmin = auth(UUID.fromString("00000000-0000-0000-0000-000000000020"), "shop.admin2", List.of("SHOP_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000020"));
        mockMvc.perform(get("/api/balances/supplier/00000000-0000-0000-0000-000000000010")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listSupplierBalances_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/balances/supplier/00000000-0000-0000-0000-000000000010"))
                .andExpect(status().isUnauthorized());
    }
}
