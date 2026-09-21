package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.valueobject.OrganizationName;
import com.paymentplatform.organization.domain.valueobject.OrganizationType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InternalOrganizationControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private OrganizationRepository organizations;

    private MockMvc mockMvc;
    private static final String INTERNAL_TOKEN = "test-internal-secret";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac).build();
    }

    private UUID createOrg(String name) {
        Organization org = Organization.create(OrganizationId.of(null), OrganizationName.of(name), OrganizationType.SHOP);
        return organizations.save(org).id().value();
    }

    @Test
    void getStatus_validToken_returnsOk() throws Exception {
        UUID orgId = createOrg("InternalTest-" + System.nanoTime());

        mockMvc.perform(get("/api/organizations/internal/" + orgId + "/status")
                        .header("X-Internal-Token", INTERNAL_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").isString());
    }

    @Test
    void getStatus_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/organizations/internal/00000000-0000-0000-0000-000000000001/status")
                        .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getStatus_missingToken_returns401() throws Exception {
        mockMvc.perform(get("/api/organizations/internal/00000000-0000-0000-0000-000000000001/status"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRelationsBySupplier_validToken_returnsOk() throws Exception {
        mockMvc.perform(get("/api/organizations/internal/relations/supplier/00000000-0000-0000-0000-000000000001")
                        .header("X-Internal-Token", INTERNAL_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getRelationsBySupplier_invalidToken_returns401() throws Exception {
        mockMvc.perform(get("/api/organizations/internal/relations/supplier/00000000-0000-0000-0000-000000000001")
                        .header("X-Internal-Token", "wrong-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getRelationsBySupplier_missingToken_returns401() throws Exception {
        mockMvc.perform(get("/api/organizations/internal/relations/supplier/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }
}
