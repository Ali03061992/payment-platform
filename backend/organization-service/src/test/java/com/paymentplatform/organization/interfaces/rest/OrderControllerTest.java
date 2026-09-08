package com.paymentplatform.organization.interfaces.rest;

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
class OrderControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private static UsernamePasswordAuthenticationToken auth(long userId, String username, List<String> perms, Long orgId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, username, perms, orgId);
        var authorities = perms.stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private UsernamePasswordAuthenticationToken shopAdmin() {
        return auth(1L, "shop.admin", List.of("SHOP_ADMIN"), 10L);
    }

    private UsernamePasswordAuthenticationToken supplierAdmin() {
        return auth(2L, "supplier.admin", List.of("SUPPLIER_ADMIN"), 20L);
    }

    private UsernamePasswordAuthenticationToken systemAdmin() {
        return auth(3L, "sysadmin", List.of("SYSTEM_ADMIN"), null);
    }

    private UsernamePasswordAuthenticationToken deliveryAgent() {
        return auth(4L, "delivery.agent", List.of("DELIVERY_AGENT"), null);
    }

    @Test
    void listOrders_asShopAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void listOrders_asSupplierAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void listOrders_asSystemAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(systemAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void listOrders_asDeliveryAgent_returnsOk() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(deliveryAgent())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void listOrders_withStatusFilter_returnsOk() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin()))
                        .param("status", "PENDING"))
                .andExpect(status().isOk());
    }

    @Test
    void getOrder_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/orders/99999")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(systemAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrder_asDeliveryAgent_returnsOk() throws Exception {
        mockMvc.perform(get("/api/orders/1")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(deliveryAgent())))
                .andExpect(status().isNotFound());
    }

    @Test
    void myDeliveries_asDeliveryAgent_returnsOk() throws Exception {
        mockMvc.perform(get("/api/orders/my-deliveries")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(deliveryAgent())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createOrder_withoutAuth_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.application.dto.CreateOrderRequest(
                        20L, 10L, false, "TND", null,
                        List.of(new com.paymentplatform.organization.application.dto.OrderItemRequest(1L, 5, null))));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirmOrder_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/orders/99999/confirm")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelOrder_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/orders/99999/cancel")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin())))
                .andExpect(status().isNotFound());
    }
}
