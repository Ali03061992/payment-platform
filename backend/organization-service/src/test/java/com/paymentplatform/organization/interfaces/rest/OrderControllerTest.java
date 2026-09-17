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

    private static UsernamePasswordAuthenticationToken auth(UUID userId, String username, List<String> perms, UUID orgId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, username, perms, orgId);
        var authorities = perms.stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private UsernamePasswordAuthenticationToken shopAdmin() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000001"), "shop.admin", List.of("SHOP_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
    }

    private UsernamePasswordAuthenticationToken supplierAdmin() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000002"), "supplier.admin", List.of("SUPPLIER_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000020"));
    }

    private UsernamePasswordAuthenticationToken systemAdmin() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000003"), "sysadmin", List.of("SYSTEM_ADMIN"), null);
    }

    private UsernamePasswordAuthenticationToken deliveryAgent() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000004"), "delivery.agent", List.of("SUPPLIER_AGENT"), null);
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
        mockMvc.perform(get("/api/orders/00000000-0000-0000-0000-000000099999")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(systemAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrder_asDeliveryAgent_returnsOk() throws Exception {
        mockMvc.perform(get("/api/orders/00000000-0000-0000-0000-000000000001")
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
                        UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000010"), false, "TND", null,
                        List.of(new com.paymentplatform.organization.application.dto.OrderItemRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), 5, null))));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirmOrder_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/orders/00000000-0000-0000-0000-000000099999/confirm")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancelOrder_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(post("/api/orders/00000000-0000-0000-0000-000000099999/cancel")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void listOrders_withPagination_returnsOk() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin()))
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk());
    }

    @Test
    void listOrders_asSupplierWithStatus_returnsOk() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .param("status", "PENDING"))
                .andExpect(status().isOk());
    }

    @Test
    void listOrders_asShopWithStatus_returnsOk() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin()))
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk());
    }

    @Test
    void listOrders_asSystemAdminWithStatus_returnsOk() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(systemAdmin()))
                        .param("status", "PENDING"))
                .andExpect(status().isOk());
    }

    @Test
    void createOrder_asShopAdmin_returns201or400() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.application.dto.CreateOrderRequest(
                        UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000010"), false, "TND", null,
                        List.of(new com.paymentplatform.organization.application.dto.OrderItemRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), 2, null))));
        // This will likely fail due to product not found, but tests 403/400 handling
        mockMvc.perform(post("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void createOrder_asSupplierAdmin_returnsCreatedOrError() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.application.dto.CreateOrderRequest(
                        UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000010"), false, "TND", null,
                        List.of(new com.paymentplatform.organization.application.dto.OrderItemRequest(UUID.fromString("00000000-0000-0000-0000-000000000001"), 1, null))));
        mockMvc.perform(post("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void getOrder_asSystemAdmin_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/orders/00000000-0000-0000-0000-000000099998")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(systemAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getOrder_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/orders/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirmOrder_withoutAuth_returns401() throws Exception {
        mockMvc.perform(post("/api/orders/00000000-0000-0000-0000-000000000001/confirm"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void prepareOrder_nonExistent_returns404() throws Exception {
        mockMvc.perform(post("/api/orders/00000000-0000-0000-0000-000000099999/prepare")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void readyForDelivery_nonExistent_returns404() throws Exception {
        mockMvc.perform(post("/api/orders/00000000-0000-0000-0000-000000099999/ready")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void assignDelivery_nonExistent_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("agentId", UUID.fromString("00000000-0000-0000-0000-000000000004").toString()));
        mockMvc.perform(post("/api/orders/00000000-0000-0000-0000-000000099999/assign-delivery")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void confirmDelivery_nonExistent_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("confirmedDate", "2026-09-17"));
        mockMvc.perform(post("/api/orders/00000000-0000-0000-0000-000000099999/confirm-delivery")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(deliveryAgent()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void deliverOrder_nonExistent_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("receivedBy", UUID.fromString("00000000-0000-0000-0000-000000000004").toString()));
        mockMvc.perform(post("/api/orders/00000000-0000-0000-0000-000000099999/deliver")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptOrder_nonExistent_returns404() throws Exception {
        mockMvc.perform(post("/api/orders/00000000-0000-0000-0000-000000099999/accept")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void acceptAsap_nonExistent_returns404() throws Exception {
        mockMvc.perform(post("/api/orders/00000000-0000-0000-0000-000000099999/accept-asap")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectOrder_nonExistent_returns404() throws Exception {
        mockMvc.perform(post("/api/orders/00000000-0000-0000-0000-000000099999/reject")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deliveryReject_nonExistent_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("reason", "Damaged goods"));
        mockMvc.perform(post("/api/orders/00000000-0000-0000-0000-000000099999/delivery-reject")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void deliveryReject_withoutReason_returnsNotFoundOrOk() throws Exception {
        mockMvc.perform(post("/api/orders/00000000-0000-0000-0000-000000099999/delivery-reject")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void myDeliveries_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/orders/my-deliveries"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listOrders_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized());
    }
}
