package com.paymentplatform.gateway;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * B3 : deny-by-default au gateway. Plus de "/api/**".permitAll() — toute route
 * API sans JWT valide est rejetée en 401 par la chaîne Spring Security
 * (SecurityContext alimenté par JwtValidationFilter).
 */
@SpringBootTest
@ActiveProfiles("test")
class GatewaySecurityTest {

    @Autowired private WebApplicationContext wac;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        // B3 : la vraie chaîne Spring Security tourne dans le test (sinon les
        // requêtes seraient réellement proxées vers les services en aval).
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
    }

    @Test
    void apiWithoutJwt_returns401() throws Exception {
        mockMvc.perform(get("/api/payments"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Token d'authentification manquant"));
    }

    @Test
    void internalRouteWithoutJwt_returns401() throws Exception {
        // B3 : les routes internal/** exigent un JWT au gateway (défense en
        // profondeur, en plus du X-Internal-Token vérifié par le service appelé).
        mockMvc.perform(get("/api/organizations/internal/00000000-0000-0000-0000-000000000001/status"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Token d'authentification manquant"));
    }

    @Test
    void actuatorHealth_staysPublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }
}
