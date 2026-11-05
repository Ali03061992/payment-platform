package com.paymentplatform.notification.infrastructure.rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoint interne appelé par le gateway après mutation : le secret
 * X-Internal-Token est obligatoire, la diffusion SSE est sans contenu métier.
 */
@SpringBootTest
@ActiveProfiles("test")
class InternalNotificationControllerTest {

    private static final String SECRET = "test-internal-secret";

    @Autowired private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    @Test
    void dataChanged_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/internal/notifications/data-changed"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dataChanged_withWrongToken_returns401() throws Exception {
        mockMvc.perform(post("/api/internal/notifications/data-changed")
                        .header("X-Internal-Token", "wrong"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void dataChanged_withValidToken_returns202() throws Exception {
        mockMvc.perform(post("/api/internal/notifications/data-changed")
                        .header("X-Internal-Token", SECRET))
                .andExpect(status().isAccepted());
    }
}
