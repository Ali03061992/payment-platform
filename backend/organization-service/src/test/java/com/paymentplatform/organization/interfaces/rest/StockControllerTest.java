package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.organization.application.dto.ProductCreateRequest;
import com.paymentplatform.organization.application.dto.ProductUpdateRequest;
import com.paymentplatform.organization.application.dto.StockMovementRequest;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.ProductRepository;
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

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StockControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private ProductRepository products;

    private MockMvc mockMvc;
    private static final long SUPPLIER_ID = 10L;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        products.deleteAll();
    }

    private static UsernamePasswordAuthenticationToken auth(long userId, String username, List<String> perms, Long orgId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, username, perms, orgId);
        var authorities = perms.stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private UsernamePasswordAuthenticationToken supplierAdmin() {
        return auth(1L, "supplier.admin", List.of("SUPPLIER_ADMIN", "SUPPLIER_MANAGE_PRODUCTS", "SUPPLIER_MANAGE_STOCK"), SUPPLIER_ID);
    }

    private Product createProduct(String sku) {
        Product p = new Product();
        p.setSupplierId(SUPPLIER_ID);
        p.setName("Product-" + sku);
        p.setSku(sku);
        p.setUnitPrice(new BigDecimal("25.00"));
        p.setCurrency("TND");
        p.setQuantity(100);
        p.setMinQuantity(10);
        p.setStatus("ACTIVE");
        p.setReservedQty(0);
        return products.save(p);
    }

    @Test
    void listProducts_returnsOk() throws Exception {
        createProduct("SKU-001");

        mockMvc.perform(get("/api/suppliers/" + SUPPLIER_ID + "/products")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void listProducts_withStatusFilter_returnsOk() throws Exception {
        createProduct("SKU-002");

        mockMvc.perform(get("/api/suppliers/" + SUPPLIER_ID + "/products")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .param("status", "ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void getProduct_existingProduct_returnsOk() throws Exception {
        Product p = createProduct("SKU-003");

        mockMvc.perform(get("/api/suppliers/" + SUPPLIER_ID + "/products/" + p.getId())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Product-SKU-003"));
    }

    @Test
    void createProduct_validRequest_returnsOk() throws Exception {
        ProductCreateRequest request = new ProductCreateRequest(
                "Widget", "SKU-NEW", "Description", new BigDecimal("15.00"), "TND", 50, 5);

        mockMvc.perform(post("/api/suppliers/" + SUPPLIER_ID + "/products")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("SKU-NEW"));
    }

    @Test
    void updateProduct_validRequest_returnsOk() throws Exception {
        Product p = createProduct("SKU-UPD");
        ProductUpdateRequest request = new ProductUpdateRequest("Updated", null, null, 200, null, null);

        mockMvc.perform(patch("/api/suppliers/" + SUPPLIER_ID + "/products/" + p.getId())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void deactivateProduct_existingProduct_returnsOk() throws Exception {
        Product p = createProduct("SKU-DEL");

        mockMvc.perform(patch("/api/suppliers/" + SUPPLIER_ID + "/products/" + p.getId() + "/deactivate")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("INACTIVE"));
    }

    @Test
    void createMovement_validRequest_returnsOk() throws Exception {
        Product p = createProduct("SKU-MOV");
        StockMovementRequest request = new StockMovementRequest(p.getId(), "IN", 50, "REF-001", "Restock");

        mockMvc.perform(post("/api/suppliers/" + SUPPLIER_ID + "/movements")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("IN"));
    }

    @Test
    void listMovements_returnsOk() throws Exception {
        mockMvc.perform(get("/api/suppliers/" + SUPPLIER_ID + "/movements")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void listProducts_wrongSupplier_returns403() throws Exception {
        UsernamePasswordAuthenticationToken other = auth(2L, "other", List.of("SUPPLIER_MANAGE_PRODUCTS"), 99L);

        mockMvc.perform(get("/api/suppliers/" + SUPPLIER_ID + "/products")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(other)))
                .andExpect(status().isForbidden());
    }

    @Test
    void listProducts_systemAdmin_returnsOk() throws Exception {
        UsernamePasswordAuthenticationToken sysAdmin = auth(3L, "sysadmin", List.of("SYSTEM_ADMIN"), null);

        mockMvc.perform(get("/api/suppliers/" + SUPPLIER_ID + "/products")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(sysAdmin)))
                .andExpect(status().isOk());
    }
}
