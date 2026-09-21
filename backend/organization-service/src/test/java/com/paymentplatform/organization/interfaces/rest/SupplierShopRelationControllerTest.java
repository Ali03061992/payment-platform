package com.paymentplatform.organization.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.CreateRelationRequest;
import com.paymentplatform.organization.application.usecase.CreateOrganizationUseCase;
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
class SupplierShopRelationControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CreateOrganizationUseCase createOrg;

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
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000001"), "admin", List.of("ADMIN_MANAGE_ORGANIZATIONS"), null);
    }

    private UUID createSupplier(String name) {
        var resp = createOrg.execute(new CreateOrganizationRequest(name + "-" + System.nanoTime(), "SUPPLIER"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return resp.id();
    }

    private UUID createShop(String name) {
        var resp = createOrg.execute(new CreateOrganizationRequest(name + "-" + System.nanoTime(), "SHOP"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        return resp.id();
    }

    @Test
    void createRelation_valid_returns201() throws Exception {
        UUID supplierId = createSupplier("RelSup");
        UUID shopId = createShop("RelShop");
        CreateRelationRequest req = new CreateRelationRequest(supplierId, shopId);
        mockMvc.perform(post("/api/admin/supplier-shop-relations")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.supplierId").value(supplierId.toString()))
                .andExpect(jsonPath("$.shopId").value(shopId.toString()));
    }

    @Test
    void createRelation_duplicate_returns409() throws Exception {
        UUID supplierId = createSupplier("DupSup");
        UUID shopId = createShop("DupShop");
        CreateRelationRequest req = new CreateRelationRequest(supplierId, shopId);
        mockMvc.perform(post("/api/admin/supplier-shop-relations")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/supplier-shop-relations")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void createRelation_withoutAuth_returns401() throws Exception {
        UUID supplierId = UUID.randomUUID();
        UUID shopId = UUID.randomUUID();
        CreateRelationRequest req = new CreateRelationRequest(supplierId, shopId);
        mockMvc.perform(post("/api/admin/supplier-shop-relations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRelation_withoutAdminRole_returns403() throws Exception {
        UUID supplierId = createSupplier("NoPermSup");
        UUID shopId = createShop("NoPermShop");
        CreateRelationRequest req = new CreateRelationRequest(supplierId, shopId);
        var shopUser = auth(UUID.fromString("00000000-0000-0000-0000-000000000002"), "shop.user", List.of("SHOP_ADMIN"), shopId);
        mockMvc.perform(post("/api/admin/supplier-shop-relations")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAll_asAdmin_returnsOk() throws Exception {
        mockMvc.perform(get("/api/admin/supplier-shop-relations")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listAll_withoutAdmin_returns403() throws Exception {
        var shopUser = auth(UUID.randomUUID(), "shop", List.of("SHOP_ADMIN"), UUID.randomUUID());
        mockMvc.perform(get("/api/admin/supplier-shop-relations")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listBySupplier_asAdmin_returnsOk() throws Exception {
        UUID supplierId = createSupplier("ListSupAdmin");
        mockMvc.perform(get("/api/admin/supplier-shop-relations/supplier/" + supplierId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listBySupplier_asSystemAdmin_returnsOk() throws Exception {
        UUID supplierId = createSupplier("ListSupSys");
        var sysAdmin = auth(UUID.randomUUID(), "sys", List.of("SYSTEM_ADMIN"), null);
        // SYSTEM_ADMIN is not directly ADMIN_MANAGE but controller PreAuthorize is isAuthenticated, and method checks isAdmin via SYSTEM_ADMIN
        // We need to authenticate as SYSTEM_ADMIN for listBySupplier
        mockMvc.perform(get("/api/admin/supplier-shop-relations/supplier/" + supplierId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(sysAdmin)))
                .andExpect(status().isOk());
    }

    @Test
    void listBySupplier_asOwner_returnsOk() throws Exception {
        UUID supplierId = createSupplier("ListSupOwner");
        var owner = auth(UUID.randomUUID(), "owner", List.of("SUPPLIER_ADMIN"), supplierId);
        mockMvc.perform(get("/api/admin/supplier-shop-relations/supplier/" + supplierId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void listBySupplier_asOtherOrg_returns403() throws Exception {
        UUID supplierId = createSupplier("ListSupOther");
        var other = auth(UUID.randomUUID(), "other", List.of("SUPPLIER_ADMIN"), UUID.randomUUID());
        mockMvc.perform(get("/api/admin/supplier-shop-relations/supplier/" + supplierId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listByShop_asOwner_returnsOk() throws Exception {
        UUID shopId = createShop("ListShopOwner");
        var owner = auth(UUID.randomUUID(), "shopOwner", List.of("SHOP_ADMIN"), shopId);
        mockMvc.perform(get("/api/admin/supplier-shop-relations/shop/" + shopId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(owner)))
                .andExpect(status().isOk());
    }

    @Test
    void listByShop_asOther_returns403() throws Exception {
        UUID shopId = createShop("ListShopOther");
        var other = auth(UUID.randomUUID(), "otherShop", List.of("SHOP_ADMIN"), UUID.randomUUID());
        mockMvc.perform(get("/api/admin/supplier-shop-relations/shop/" + shopId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listByShop_asSystemAdmin_returnsOk() throws Exception {
        UUID shopId = createShop("ListShopSys");
        var sysAdmin = auth(UUID.randomUUID(), "sysAdmin", List.of("SYSTEM_ADMIN"), null);
        mockMvc.perform(get("/api/admin/supplier-shop-relations/shop/" + shopId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(sysAdmin)))
                .andExpect(status().isOk());
    }

    @Test
    void deactivate_existing_returns204() throws Exception {
        UUID supplierId = createSupplier("DeactSup");
        UUID shopId = createShop("DeactShop");
        CreateRelationRequest req = new CreateRelationRequest(supplierId, shopId);
        String result = mockMvc.perform(post("/api/admin/supplier-shop-relations")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID relationId = UUID.fromString(objectMapper.readTree(result).get("id").asText());

        mockMvc.perform(delete("/api/admin/supplier-shop-relations/" + relationId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isNoContent());
    }

    @Test
    void deactivate_nonExistent_returns404() throws Exception {
        mockMvc.perform(delete("/api/admin/supplier-shop-relations/00000000-0000-0000-0000-999999999999")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deactivate_withoutAdmin_returns403() throws Exception {
        var shopUser = auth(UUID.randomUUID(), "shop", List.of("SHOP_ADMIN"), UUID.randomUUID());
        mockMvc.perform(delete("/api/admin/supplier-shop-relations/00000000-0000-0000-0000-000000000001")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopUser)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listBySupplier_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/supplier-shop-relations/supplier/00000000-0000-0000-0000-000000000001"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createRelation_withInvalidSupplierType_returns409() throws Exception {
        UUID shopAsSupplier = createShop("ShopAsSupplier");
        UUID shopId = createShop("RealShop");
        CreateRelationRequest req = new CreateRelationRequest(shopAsSupplier, shopId);
        mockMvc.perform(post("/api/admin/supplier-shop-relations")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(adminUser()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }
}
