package com.paymentplatform.payment.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.payment.application.dto.CreatePaymentRequest;
import com.paymentplatform.payment.application.dto.RejectPaymentRequest;
import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.payment.domain.valueobject.Money;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
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
@Import(com.paymentplatform.payment.infrastructure.http.TestOrganizationValidationConfig.class)
class PaymentControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PaymentRepository payments;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    private Payment createPayment(UUID shopId, UUID supplierId, UUID createdBy) {
        return payments.save(Payment.create(
                shopId, supplierId,
                Money.of(new BigDecimal("100.00"), "TND"),
                createdBy));
    }

    private static UsernamePasswordAuthenticationToken auth(UUID userId, String username, List<String> perms, UUID orgId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, username, perms, orgId);
        var authorities = perms.stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private UsernamePasswordAuthenticationToken shopUser() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000001"), "shop.user", List.of("SHOP_CREATE_PAYMENTS", "SHOP_CANCEL_PAYMENTS", "VIEW_PAYMENTS"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
    }

    private UsernamePasswordAuthenticationToken supplierUser() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000002"), "supplier.user", List.of("SUPPLIER_MANAGE_PAYMENTS", "VIEW_PAYMENTS"), UUID.fromString("00000000-0000-0000-0000-000000000020"));
    }

    private UsernamePasswordAuthenticationToken adminUser() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000003"), "admin", List.of("SYSTEM_ADMIN", "VIEW_PAYMENTS"), null);
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), new BigDecimal("150.00"), "TND");

        mockMvc.perform(post("/api/payments")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reference").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void getById_asShopOwner_returnsPayment() throws Exception {
        Payment p = createPayment(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000001"));

        mockMvc.perform(get("/api/payments/" + p.id())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reference").value(p.reference().value()));
    }

    @Test
    void getById_asAdmin_returnsPayment() throws Exception {
        Payment p = createPayment(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000001"));

        mockMvc.perform(get("/api/payments/" + p.id())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk());
    }

    @Test
    void list_shopRole_returnsShopPayments() throws Exception {
        createPayment(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000001"));

        mockMvc.perform(get("/api/payments")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void stats_returnsStats() throws Exception {
        mockMvc.perform(get("/api/payments/stats")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk());
    }

    @Test
    void confirm_asSupplier() throws Exception {
        Payment p = createPayment(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000001"));

        mockMvc.perform(post("/api/payments/" + p.id() + "/confirm")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void reject_asSupplier() throws Exception {
        Payment p = createPayment(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000001"));

        RejectPaymentRequest request = new RejectPaymentRequest("Quality issue");
        mockMvc.perform(post("/api/payments/" + p.id() + "/reject")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void cancel_asShop() throws Exception {
        Payment p = createPayment(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000001"));

        mockMvc.perform(post("/api/payments/" + p.id() + "/cancel")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void create_invalidAmount_returns400() throws Exception {
        CreatePaymentRequest request = new CreatePaymentRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), BigDecimal.ZERO, "TND");

        mockMvc.perform(post("/api/payments")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_missingCurrency_returns400() throws Exception {
        String request = """
                {"shopId":"00000000-0000-0000-0000-000000000010","supplierId":"00000000-0000-0000-0000-000000000020","amount":100.00}
                """;

        mockMvc.perform(post("/api/payments")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isBadRequest());
    }

    @Test
    void agentSummary_asAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/api/payments/agent-summary")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .param("supplierId", "00000000-0000-0000-0000-000000000020")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void agentSummary_asWrongSupplier_returns403() throws Exception {
        UsernamePasswordAuthenticationToken otherSupplier = auth(UUID.fromString("00000000-0000-0000-0000-000000000004"), "other", List.of("SUPPLIER_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000099"));

        mockMvc.perform(get("/api/payments/agent-summary")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(otherSupplier))
                        .param("supplierId", "00000000-0000-0000-0000-000000000020")
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isForbidden());
    }

    @Test
    void exportCsv_asAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/api/payments/export")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    void exportCsv_asShop_returnsOk() throws Exception {
        mockMvc.perform(get("/api/payments/export")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser()))
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    void exportCsv_asSupplier_returnsOk() throws Exception {
        mockMvc.perform(get("/api/payments/export")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierUser()))
                        .param("from", "2026-01-01")
                        .param("to", "2026-12-31"))
                .andExpect(status().isOk());
    }

    @Test
    void list_withStatusFilter_returnsOk() throws Exception {
        createPayment(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000001"));

        mockMvc.perform(get("/api/payments")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser()))
                        .param("status", "PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray());
    }

    @Test
    void confirm_nonExistentPayment_returns404() throws Exception {
        mockMvc.perform(post("/api/payments/00000000-0000-0000-0000-000000099999/confirm")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancel_nonExistentPayment_returns404() throws Exception {
        mockMvc.perform(post("/api/payments/00000000-0000-0000-0000-000000099999/cancel")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    void reject_nonExistentPayment_returns404() throws Exception {
        RejectPaymentRequest request = new RejectPaymentRequest("Reason");
        mockMvc.perform(post("/api/payments/00000000-0000-0000-0000-000000099999/reject")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getById_nonExistentPayment_returns404() throws Exception {
        mockMvc.perform(get("/api/payments/00000000-0000-0000-0000-000000099999")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser())))
                .andExpect(status().isNotFound());
    }
}
