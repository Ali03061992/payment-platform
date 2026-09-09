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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
}
