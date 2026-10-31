package com.paymentplatform.organization.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.usecase.CreateOrderUseCase;
import com.paymentplatform.organization.application.usecase.CreateOrganizationUseCase;
import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.usecase.SupplierShopRelationUseCase;
import com.paymentplatform.organization.application.dto.CreateRelationRequest;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.ProductRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de DisputeControllerTest.
 * Perimetre : ouverture de reclamations a tous les roles metier + isolation tenant.
 * Moyens : contexte SpringBootTest, MockMvc, profil "test" (H2).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class DisputeControllerTest {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CreateOrganizationUseCase createOrg;
    @Autowired private SupplierShopRelationUseCase relationUseCase;
    @Autowired private CreateOrderUseCase createOrder;
    @Autowired private OrderRepository orders;
    @Autowired private ProductRepository products;

    private MockMvc mockMvc;
    private UUID supplierId;
    private UUID shopId;
    private UUID foreignShopId;
    private UUID productId;
    private UUID rejectedOrderId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();
        var supplier = createOrg.execute(new CreateOrganizationRequest("Dispute Supplier", "SUPPLIER", "1234567A/A/M/000", "PAT-TEST", "Adresse Test, Tunis", null, null, null, null, null, null), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        supplierId = supplier.id();
        var shop = createOrg.execute(new CreateOrganizationRequest("Dispute Shop", "SHOP", "1234567A/A/M/000", "PAT-TEST", "Adresse Test, Tunis", null, null, null, null, null, null), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        shopId = shop.id();
        var foreign = createOrg.execute(new CreateOrganizationRequest("Foreign Shop", "SHOP", "1234567B/A/M/000", "PAT-TEST", "Adresse Test, Tunis", null, null, null, null, null, null), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        foreignShopId = foreign.id();
        relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));

        Product product = new Product();
        product.setSupplierId(supplierId);
        product.setName("Dispute Product");
        product.setSku("DP-001");
        product.setUnitPrice(new BigDecimal("10.00"));
        product.setCurrency("TND");
        product.setQuantity(100);
        product.setMinQuantity(1);
        product.setReservedQty(0);
        productId = products.save(product).getId();

        var response = createOrder.execute(
                new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, "Order a rejeter",
                        List.of(new OrderItemRequest(productId, 1, null))),
                UUID.fromString("00000000-0000-0000-0000-000000000010"), "SHOP");
        Order order = orders.findById(response.id()).orElseThrow();
        order.confirm();
        order.prepare();
        order.readyForDelivery();
        order.deliveryReject("Livraison refusee");
        rejectedOrderId = orders.save(order).getId();
    }

    private static UsernamePasswordAuthenticationToken auth(UUID userId, String username, List<String> perms, UUID orgId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, username, perms, orgId);
        var authorities = perms.stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private String body(UUID orderId, String reason) throws Exception {
        return objectMapper.writeValueAsString(
                new com.paymentplatform.organization.application.dto.CreateDisputeRequest(orderId, reason));
    }

    @Test
    void create_asShopAgent_returns201() throws Exception {
        var agent = auth(UUID.fromString("00000000-0000-0000-0000-000000000021"), "shop.agent", List.of("SHOP_AGENT"), shopId);
        mockMvc.perform(post("/api/disputes")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(rejectedOrderId, "Produit endommage")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(rejectedOrderId.toString()))
                .andExpect(jsonPath("$.reason").value("Produit endommage"));
    }

    @Test
    void create_asSupplierAgent_returns201() throws Exception {
        var agent = auth(UUID.fromString("00000000-0000-0000-0000-000000000022"), "supplier.agent", List.of("SUPPLIER_AGENT"), supplierId);
        mockMvc.perform(post("/api/disputes")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(rejectedOrderId, "Litige fournisseur")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reason").value("Litige fournisseur"));
    }

    @Test
    void create_asForeignShop_returns403() throws Exception {
        var foreignAdmin = auth(UUID.fromString("00000000-0000-0000-0000-000000000023"), "foreign.admin", List.of("SHOP_ADMIN"), foreignShopId);
        mockMvc.perform(post("/api/disputes")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(foreignAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(rejectedOrderId, "Tentative hors perimetre")))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_onNonRejectedOrder_returns409() throws Exception {
        var response = createOrder.execute(
                new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, "Commande non rejetee",
                        List.of(new OrderItemRequest(productId, 1, null))),
                UUID.fromString("00000000-0000-0000-0000-000000000010"), "SHOP");
        var shopAdmin = auth(UUID.fromString("00000000-0000-0000-0000-000000000024"), "shop.admin", List.of("SHOP_ADMIN"), shopId);
        mockMvc.perform(post("/api/disputes")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(response.id(), "Motif premature")))
                .andExpect(status().isConflict());
    }

    @Test
    void list_asSupplierAgent_returnsOk() throws Exception {
        var agent = auth(UUID.fromString("00000000-0000-0000-0000-000000000025"), "supplier.agent", List.of("SUPPLIER_AGENT"), supplierId);
        mockMvc.perform(get("/api/disputes")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }
}
