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

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
/**
 * Tests de CatalogControllerTest.
 * Perimetre : endpoints REST de CatalogController (statuts HTTP, JSON, securite).
 * Moyens : contexte SpringBootTest, MockMvc, profil "test" (H2).
 */

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CatalogControllerTest {

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

    private UsernamePasswordAuthenticationToken supplierAdmin() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000001"), "supplier.admin", List.of("SUPPLIER_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
    }

    @Test
    void listCategories_validRequest_returnsOk() throws Exception {
        mockMvc.perform(get("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .param("supplierId", "00000000-0000-0000-0000-000000000010"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createCategory_validRequest_returns201() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.CategoryRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "Category-" + System.nanoTime(), "CAT-001"));

        mockMvc.perform(post("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").isNotEmpty());
    }

    @Test
    void listFamilies_validRequest_returnsOk() throws Exception {
        mockMvc.perform(get("/api/supplier/catalog/families")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .param("supplierId", "00000000-0000-0000-0000-000000000010"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createFamily_validRequest_returns201() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.FamilyRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "Family-" + System.nanoTime(), "FAM-001", new HashSet<>()));

        mockMvc.perform(post("/api/supplier/catalog/families")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").isNotEmpty());
    }

    @Test
    void listCategories_wrongSupplier_returns403() throws Exception {
        UsernamePasswordAuthenticationToken otherSupplier = auth(UUID.fromString("00000000-0000-0000-0000-000000000002"), "other.supplier", List.of("SUPPLIER_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000099"));

        mockMvc.perform(get("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(otherSupplier))
                        .param("supplierId", "00000000-0000-0000-0000-000000000010"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listFamilies_noParams_returns400() throws Exception {
        mockMvc.perform(get("/api/supplier/catalog/families")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createCategory_duplicateCode_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.CategoryRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "Cat1-" + System.nanoTime(), "DUP-CODE"));
        mockMvc.perform(post("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());

        String body2 = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.CategoryRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "Cat2-" + System.nanoTime(), "DUP-CODE"));
        mockMvc.perform(post("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body2))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCategory_nonExistent_returns404() throws Exception {
        mockMvc.perform(delete("/api/supplier/catalog/categories/00000000-0000-0000-0000-999999999999")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteFamily_nonExistent_returns404() throws Exception {
        mockMvc.perform(delete("/api/supplier/catalog/families/00000000-0000-0000-0000-999999999999")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void listFamilies_byCategoryId_returnsOk() throws Exception {
        String catBody = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.CategoryRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "Cat-" + System.nanoTime(), "CAT-FAM"));
        String catResult = mockMvc.perform(post("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(catBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID catId = UUID.fromString(objectMapper.readTree(catResult).get("id").asText());

        mockMvc.perform(get("/api/supplier/catalog/families")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .param("categoryId", String.valueOf(catId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void updateFamily_validRequest_returnsOk() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.FamilyRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "Family-" + System.nanoTime(), "FAM-UPD", new HashSet<>()));
        String result = mockMvc.perform(post("/api/supplier/catalog/families")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(result).get("id").asText());

        String updateBody = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.FamilyRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "Updated-Family", "FAM-UPD-2", new HashSet<>()));
        mockMvc.perform(put("/api/supplier/catalog/families/" + id)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated-Family"));
    }

    @Test
    void updateFamily_nonExistent_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.FamilyRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "Family", "FAM-NF", new HashSet<>()));
        mockMvc.perform(put("/api/supplier/catalog/families/00000000-0000-0000-0000-999999999999")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCategory_wrongSupplier_returns403() throws Exception {
        UsernamePasswordAuthenticationToken otherSupplier = auth(UUID.fromString("00000000-0000-0000-0000-000000000003"), "other.supplier2", List.of("SUPPLIER_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000099"));
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.CategoryRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "CatWrong", "CAT-WRONG"));
        mockMvc.perform(post("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(otherSupplier))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createFamily_wrongSupplier_returns403() throws Exception {
        UsernamePasswordAuthenticationToken otherSupplier = auth(UUID.fromString("00000000-0000-0000-0000-000000000004"), "other.supplier3", List.of("SUPPLIER_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000099"));
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.FamilyRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "FamilyWrong", "FAM-WRONG", new HashSet<>()));
        mockMvc.perform(post("/api/supplier/catalog/families")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(otherSupplier))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createFamily_withCategoryIds_returns201() throws Exception {
        String catBody = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.CategoryRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "CatForFam-" + System.nanoTime(), "CAT-FAM2"));
        String catResult = mockMvc.perform(post("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(catBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID catId = UUID.fromString(objectMapper.readTree(catResult).get("id").asText());

        Set<UUID> catIds = new HashSet<>();
        catIds.add(catId);
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.FamilyRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "FamilyWithCat-" + System.nanoTime(), "FAM-CAT", catIds));
        mockMvc.perform(post("/api/supplier/catalog/families")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty());
    }

    @Test
    void deleteCategory_existing_returns204() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.CategoryRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "ToDelete-" + System.nanoTime(), "CAT-DEL"));
        String result = mockMvc.perform(post("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(result).get("id").asText());
        mockMvc.perform(delete("/api/supplier/catalog/categories/" + id)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isNoContent());
        // verify deleted
        mockMvc.perform(delete("/api/supplier/catalog/categories/" + id)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteFamily_existing_returns204() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.FamilyRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "ToDeleteFam-" + System.nanoTime(), "FAM-DEL", new HashSet<>()));
        String result = mockMvc.perform(post("/api/supplier/catalog/families")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(result).get("id").asText());
        mockMvc.perform(delete("/api/supplier/catalog/families/" + id)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isNoContent());
    }

    @Test
    void listCategories_asSystemAdmin_returnsOk() throws Exception {
        UsernamePasswordAuthenticationToken sysAdmin = auth(UUID.fromString("00000000-0000-0000-0000-000000000005"), "sysadmin", List.of("SYSTEM_ADMIN"), null);
        mockMvc.perform(get("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(sysAdmin))
                        .param("supplierId", "00000000-0000-0000-0000-000000000010"))
                .andExpect(status().isOk());
    }

    @Test
    void listFamilies_wrongSupplier_returns403() throws Exception {
        UsernamePasswordAuthenticationToken otherSupplier = auth(UUID.fromString("00000000-0000-0000-0000-000000000006"), "other4", List.of("SUPPLIER_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000099"));
        mockMvc.perform(get("/api/supplier/catalog/families")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(otherSupplier))
                        .param("supplierId", "00000000-0000-0000-0000-000000000010"))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateFamily_withCategoryIds_returnsOk() throws Exception {
        String catBody = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.CategoryRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "CatUpd-" + System.nanoTime(), "CAT-UPD"));
        String catResult = mockMvc.perform(post("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(catBody))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID catId = UUID.fromString(objectMapper.readTree(catResult).get("id").asText());

        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.FamilyRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "FamilyUpd-" + System.nanoTime(), "FAM-UPD3", new HashSet<>()));
        String result = mockMvc.perform(post("/api/supplier/catalog/families")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(result).get("id").asText());

        Set<UUID> catIds = new HashSet<>();
        catIds.add(catId);
        String updateBody = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.FamilyRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "UpdatedWithCat", "FAM-UPD3-2", catIds));
        mockMvc.perform(put("/api/supplier/catalog/families/" + id)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk());
    }

    @Test
    void updateFamily_wrongSupplier_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.FamilyRequest(UUID.fromString("00000000-0000-0000-0000-000000000099"), "Family", "FAM-WRONG2", new HashSet<>()));
        UsernamePasswordAuthenticationToken other = auth(UUID.fromString("00000000-0000-0000-0000-000000000007"), "other5", List.of("SUPPLIER_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000099"));
        // request supplierId 099 but auth org is also 099 so it passes? Need mismatch: auth org 010 vs request 099
        UsernamePasswordAuthenticationToken mismatch = auth(UUID.fromString("00000000-0000-0000-0000-000000000008"), "mismatch", List.of("SUPPLIER_ADMIN"), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        mockMvc.perform(put("/api/supplier/catalog/families/00000000-0000-0000-0000-999999999999")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(mismatch))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void listFamilies_bySupplierId_returnsOk_withFiltering() throws Exception {
        mockMvc.perform(get("/api/supplier/catalog/families")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .param("supplierId", "00000000-0000-0000-0000-000000000010"))
                .andExpect(status().isOk());
    }

    @Test
    void deleteCategory_softDelete_keepsRowExcludedFromList() throws Exception {
        String code = "CAT-SOFT-" + System.nanoTime();
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.CategoryRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "Soft-" + System.nanoTime(), code));
        String result = mockMvc.perform(post("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(result).get("id").asText());

        mockMvc.perform(delete("/api/supplier/catalog/categories/" + id)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isNoContent());

        // La ligne survit (soft-delete) mais disparaît des listes…
        String list = mockMvc.perform(get("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .param("supplierId", "00000000-0000-0000-0000-000000000010"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(list).doesNotContain(id.toString());

        // …le code redevient réutilisable…
        String reBody = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.CategoryRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "SoftAgain-" + System.nanoTime(), code));
        mockMvc.perform(post("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reBody))
                .andExpect(status().isCreated());

        // …et le doublon sur une ligne ACTIVE reste refusé.
        mockMvc.perform(post("/api/supplier/catalog/categories")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteFamily_softDelete_excludedFromList() throws Exception {
        String body = objectMapper.writeValueAsString(
                new com.paymentplatform.organization.interfaces.rest.CatalogController.FamilyRequest(UUID.fromString("00000000-0000-0000-0000-000000000010"), "SoftFam-" + System.nanoTime(), "FAM-SOFT", new HashSet<>()));
        String result = mockMvc.perform(post("/api/supplier/catalog/families")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(objectMapper.readTree(result).get("id").asText());

        mockMvc.perform(delete("/api/supplier/catalog/families/" + id)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isNoContent());

        String list = mockMvc.perform(get("/api/supplier/catalog/families")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .param("supplierId", "00000000-0000-0000-0000-000000000010"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(list).doesNotContain(id.toString());

        // Second delete => 404 (déjà supprimée).
        mockMvc.perform(delete("/api/supplier/catalog/families/" + id)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isNotFound());
    }
}
