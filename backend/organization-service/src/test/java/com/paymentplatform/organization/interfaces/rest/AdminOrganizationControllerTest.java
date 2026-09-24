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

import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AdminOrganizationControllerTest {

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

    private UsernamePasswordAuthenticationToken adminUser() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000001"), "admin", List.of("ADMIN_MANAGE_ORGANIZATIONS", "ADMIN_VIEW_STATS"), null);
    }

    @Test
    void createSupplier_validRequest_returns201() throws Exception {
        String name = "Supplier-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(new com.paymentplatform.organization.application.dto.CreateOrganizationRequest(name, "SUPPLIER"));

        mockMvc.perform(post("/api/admin/suppliers")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("SUPPLIER"));
    }

    @Test
    void listSuppliers_returnsList() throws Exception {
        mockMvc.perform(get("/api/admin/suppliers")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void listSuppliers_paginationRespectsSize() throws Exception {
        mockMvc.perform(get("/api/admin/suppliers")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.number").value(0));
    }

    @Test
    void createShop_validRequest_returns201() throws Exception {
        String name = "Shop-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(new com.paymentplatform.organization.application.dto.CreateOrganizationRequest(name, "SHOP"));

        mockMvc.perform(post("/api/admin/shops")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(name))
                .andExpect(jsonPath("$.type").value("SHOP"));
    }

    @Test
    void listShops_returnsList() throws Exception {
        mockMvc.perform(get("/api/admin/shops")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.totalElements").isNumber());
    }

    @Test
    void stats_returnsStats() throws Exception {
        mockMvc.perform(get("/api/admin/stats")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suppliers").isNumber())
                .andExpect(jsonPath("$.shops").isNumber());
    }

    @Test
    void createSupplier_withoutAuth_returns401() throws Exception {
        String body = objectMapper.writeValueAsString(new com.paymentplatform.organization.application.dto.CreateOrganizationRequest("Test", "SUPPLIER"));

        mockMvc.perform(post("/api/admin/suppliers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listSuppliers_withWrongRole_returns403() throws Exception {
        UsernamePasswordAuthenticationToken shopUser = auth(UUID.fromString("00000000-0000-0000-0000-000000000002"), "shop.user", List.of("SHOP_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000010"));

        mockMvc.perform(get("/api/admin/suppliers")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getSupplier_existing_returns200() throws Exception {
        String name = "Get-Supplier-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(new com.paymentplatform.organization.application.dto.CreateOrganizationRequest(name, "SUPPLIER"));
        String result = mockMvc.perform(post("/api/admin/suppliers")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(result).get("id").asText();

        mockMvc.perform(get("/api/admin/suppliers/" + id)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.name").value(name));
    }

    @Test
    void getSupplier_nonExistent_returns404() throws Exception {
        mockMvc.perform(get("/api/admin/suppliers/00000000-0000-0000-0000-999999999999")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    void getShop_existing_returns200() throws Exception {
        String name = "Get-Shop-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(new com.paymentplatform.organization.application.dto.CreateOrganizationRequest(name, "SHOP"));
        String result = mockMvc.perform(post("/api/admin/shops")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(result).get("id").asText();

        mockMvc.perform(get("/api/admin/shops/" + id)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void activateSupplier_existing_returns200() throws Exception {
        String name = "Activate-Supplier-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(new com.paymentplatform.organization.application.dto.CreateOrganizationRequest(name, "SUPPLIER"));
        String result = mockMvc.perform(post("/api/admin/suppliers")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(result).get("id").asText();

        // disable then activate
        mockMvc.perform(patch("/api/admin/suppliers/" + id + "/disable")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/suppliers/" + id + "/activate")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void disableSupplier_existing_returns200() throws Exception {
        String name = "Disable-Supplier-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(new com.paymentplatform.organization.application.dto.CreateOrganizationRequest(name, "SUPPLIER"));
        String result = mockMvc.perform(post("/api/admin/suppliers")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(result).get("id").asText();

        mockMvc.perform(patch("/api/admin/suppliers/" + id + "/disable")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk());
    }

    @Test
    void activateShop_existing_returns200() throws Exception {
        String name = "Activate-Shop-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(new com.paymentplatform.organization.application.dto.CreateOrganizationRequest(name, "SHOP"));
        String result = mockMvc.perform(post("/api/admin/shops")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(result).get("id").asText();

        mockMvc.perform(patch("/api/admin/shops/" + id + "/disable")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/admin/shops/" + id + "/activate")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk());
    }

    @Test
    void disableShop_existing_returns200() throws Exception {
        String name = "Disable-Shop-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(new com.paymentplatform.organization.application.dto.CreateOrganizationRequest(name, "SHOP"));
        String result = mockMvc.perform(post("/api/admin/shops")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn().getResponse().getContentAsString();
        String id = objectMapper.readTree(result).get("id").asText();

        mockMvc.perform(patch("/api/admin/shops/" + id + "/disable")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk());
    }

    @Test
    void activateSupplier_withoutAdminRole_returns403() throws Exception {
        UsernamePasswordAuthenticationToken shopUser = auth(UUID.fromString("00000000-0000-0000-0000-000000000005"), "shop.user2", List.of("SHOP_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        mockMvc.perform(patch("/api/admin/suppliers/00000000-0000-0000-0000-000000000099/activate")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listShops_withAllowedShopRole_returnsOk() throws Exception {
        UsernamePasswordAuthenticationToken shopUser = auth(UUID.fromString("00000000-0000-0000-0000-000000000006"), "shop.create", List.of("SHOP_CREATE_PAYMENTS"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        mockMvc.perform(get("/api/admin/shops")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser)))
                .andExpect(status().isOk());
    }

    @Test
    void createShop_withoutAdminRole_returns403() throws Exception {
        UsernamePasswordAuthenticationToken shopUser = auth(UUID.fromString("00000000-0000-0000-0000-000000000007"), "shop.user3", List.of("SHOP_ADMIN"), null);
        String body = objectMapper.writeValueAsString(new com.paymentplatform.organization.application.dto.CreateOrganizationRequest("ShouldFail", "SHOP"));
        mockMvc.perform(post("/api/admin/shops")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void stats_withoutCorrectAuthority_returns403() throws Exception {
        UsernamePasswordAuthenticationToken shopUser = auth(UUID.fromString("00000000-0000-0000-0000-000000000008"), "shop.user4", List.of("SHOP_ADMIN"), null);
        mockMvc.perform(get("/api/admin/stats")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser)))
                .andExpect(status().isForbidden());
    }
}
