package com.paymentplatform.payment.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * Tests de InternalPaymentControllerTest.
 * Perimetre : endpoints REST de InternalPaymentController (statuts HTTP, JSON, securite).
 * Moyens : contexte SpringBootTest, MockMvc, profil "test" (H2).
 */

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Import(com.paymentplatform.payment.infrastructure.http.TestOrganizationValidationConfig.class)
class InternalPaymentControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;
    private static final String INTERNAL_TOKEN = "test-internal-secret";
    private static final String ACTOR = "00000000-0000-0000-0000-000000000010";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    private String body(UUID orderId) {
        return "{\"shopId\":\"00000000-0000-0000-0000-000000000010\","
                + "\"supplierId\":\"00000000-0000-0000-0000-000000000020\","
                + "\"amount\":100.00,\"currency\":\"TND\","
                + "\"orderId\":\"" + orderId + "\",\"dueDate\":null}";
    }

    @Test
    void auto_validToken_returns201() throws Exception {
        mockMvc.perform(post("/api/internal/payments/auto")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .header("X-Actor-User-Id", ACTOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UUID.randomUUID())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void auto_sameOrderId_isIdempotent() throws Exception {
        UUID orderId = UUID.randomUUID();
        String first = mockMvc.perform(post("/api/internal/payments/auto")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .header("X-Actor-User-Id", ACTOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(orderId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String firstId = objectMapper.readTree(first).get("id").asText();

        // Rejeu (livraison + accept-asap) => même paiement, pas de doublon.
        mockMvc.perform(post("/api/internal/payments/auto")
                        .header("X-Internal-Token", INTERNAL_TOKEN)
                        .header("X-Actor-User-Id", ACTOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(orderId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(firstId));
    }

    @Test
    void auto_wrongToken_returns401() throws Exception {
        mockMvc.perform(post("/api/internal/payments/auto")
                        .header("X-Internal-Token", "wrong-token")
                        .header("X-Actor-User-Id", ACTOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void auto_missingToken_returns401() throws Exception {
        mockMvc.perform(post("/api/internal/payments/auto")
                        .header("X-Actor-User-Id", ACTOR)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(UUID.randomUUID())))
                .andExpect(status().isUnauthorized());
    }
}
