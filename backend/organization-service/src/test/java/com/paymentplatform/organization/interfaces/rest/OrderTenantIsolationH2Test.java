package com.paymentplatform.organization.interfaces.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.CreateRelationRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.usecase.CreateOrderUseCase;
import com.paymentplatform.organization.application.usecase.CreateOrganizationUseCase;
import com.paymentplatform.organization.application.usecase.SupplierShopRelationUseCase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests d'isolation multi-tenant des endpoints commandes et litiges.
 * Un compte d'une organisation tierce ne doit jamais pouvoir lire ni agir
 * sur une commande d'une autre relation fournisseur/boutique.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderTenantIsolationH2Test {

    @Autowired private WebApplicationContext wac;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private CreateOrganizationUseCase createOrg;
    @Autowired private SupplierShopRelationUseCase relationUseCase;
    @Autowired private CreateOrderUseCase createOrder;
    @Autowired private ProductRepository products;
    @Autowired private OrderRepository orders;

    private MockMvc mockMvc;
    private UUID supplierId;
    private UUID shopId;
    private UUID foreignSupplierId;
    private UUID foreignShopId;
    private UUID draftOrderId;

    private static final UUID SEED_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID DRAFT_ORDER_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        supplierId = createOrg.execute(new CreateOrganizationRequest(
                "Iso Supplier", "SUPPLIER", "2000001A/A/M/000", "PAT-ISO-1",
                "Adresse Test, Tunis", null, null, null, null, null, null), SEED_ACTOR).id();
        shopId = createOrg.execute(new CreateOrganizationRequest(
                "Iso Shop", "SHOP", "2000001B/A/M/000", "PAT-ISO-2",
                "Adresse Test, Tunis", null, null, null, null, null, null), SEED_ACTOR).id();
        foreignSupplierId = createOrg.execute(new CreateOrganizationRequest(
                "Foreign Supplier", "SUPPLIER", "2000002A/A/M/000", "PAT-ISO-3",
                "Adresse Test, Sfax", null, null, null, null, null, null), SEED_ACTOR).id();
        foreignShopId = createOrg.execute(new CreateOrganizationRequest(
                "Foreign Shop", "SHOP", "2000002B/A/M/000", "PAT-ISO-4",
                "Adresse Test, Sfax", null, null, null, null, null, null), SEED_ACTOR).id();

        relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));

        Product product = new Product();
        product.setSupplierId(supplierId);
        product.setName("Iso Product");
        product.setSku("ISO-001");
        product.setUnitPrice(new BigDecimal("10.00"));
        product.setCurrency("TND");
        product.setQuantity(100);
        product.setMinQuantity(1);
        product.setReservedQty(0);
        UUID productId = products.save(product).getId();

        draftOrderId = createOrder.execute(
                new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, "Commande isolation",
                        List.of(new OrderItemRequest(productId, 1, null))),
                DRAFT_ORDER_ACTOR, "SHOP").id();
    }

    private UUID createRejectedOrder() {
        Product product = new Product();
        product.setSupplierId(supplierId);
        product.setName("Iso Rejected");
        product.setSku("ISO-REJ");
        product.setUnitPrice(new BigDecimal("10.00"));
        product.setCurrency("TND");
        product.setQuantity(100);
        product.setMinQuantity(1);
        product.setReservedQty(0);
        UUID productId = products.save(product).getId();

        UUID orderId = createOrder.execute(
                new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, "Commande rejetee",
                        List.of(new OrderItemRequest(productId, 1, null))),
                DRAFT_ORDER_ACTOR, "SHOP").id();
        Order order = orders.findById(orderId).orElseThrow();
        order.confirm();
        order.prepare();
        order.readyForDelivery();
        order.deliveryReject("Livraison refusee");
        return orders.save(order).getId();
    }

    private static UsernamePasswordAuthenticationToken auth(UUID userId, String username,
                                                             List<String> perms, UUID orgId) {
        AuthenticatedUser principal = new AuthenticatedUser(userId, username, perms, orgId);
        var authorities = perms.stream().map(SimpleGrantedAuthority::new).toList();
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }

    private UsernamePasswordAuthenticationToken shopAdmin() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000031"),
                "iso.shop.admin", List.of("SHOP_ADMIN"), shopId);
    }

    private UsernamePasswordAuthenticationToken supplierAdmin() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000032"),
                "iso.supplier.admin", List.of("SUPPLIER_ADMIN"), supplierId);
    }

    private UsernamePasswordAuthenticationToken foreignShopAdmin() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000033"),
                "foreign.shop.admin", List.of("SHOP_ADMIN"), foreignShopId);
    }

    private UsernamePasswordAuthenticationToken foreignSupplierAdmin() {
        return auth(UUID.fromString("00000000-0000-0000-0000-000000000034"),
                "foreign.supplier.admin", List.of("SUPPLIER_ADMIN"), foreignSupplierId);
    }

    private String disputeBody(UUID orderId) throws Exception {
        return objectMapper.writeValueAsString(
                new com.paymentplatform.organization.application.dto.CreateDisputeRequest(
                        orderId, "Litige hors perimetre"));
    }

    @Test
    void getOrder_asForeignShop_returns403() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", draftOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(foreignShopAdmin())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrder_asOwningShop_returns200() throws Exception {
        mockMvc.perform(get("/api/orders/{id}", draftOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(draftOrderId.toString()));
    }

    @Test
    void confirmOrder_asForeignSupplier_returns403() throws Exception {
        mockMvc.perform(post("/api/orders/{id}/confirm", draftOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(foreignSupplierAdmin())))
                .andExpect(status().isForbidden());
    }

    @Test
    void confirmOrder_asOwningSupplier_returns200() throws Exception {
        mockMvc.perform(post("/api/orders/{id}/confirm", draftOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void cancelOrder_asForeignShop_returns403() throws Exception {
        mockMvc.perform(post("/api/orders/{id}/cancel", draftOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(foreignShopAdmin())))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelOrder_asOwningShop_returns200() throws Exception {
        mockMvc.perform(post("/api/orders/{id}/cancel", draftOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void acceptOrder_asForeignShop_returns403() throws Exception {
        mockMvc.perform(post("/api/orders/{id}/accept", draftOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(foreignShopAdmin())))
                .andExpect(status().isForbidden());
    }

    @Test
    void assignDelivery_asForeignSupplier_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "agentId", UUID.fromString("00000000-0000-0000-0000-000000000099").toString()));
        mockMvc.perform(post("/api/orders/{id}/assign-delivery", draftOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(foreignSupplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateExternalReference_asForeignSupplier_returns403() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("reference", "TEJ-FOREIGN"));
        mockMvc.perform(patch("/api/orders/{id}/external-reference", draftOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(foreignSupplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateExternalReference_asOwningSupplier_returns200() throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of("reference", "TEJ-OWNER"));
        mockMvc.perform(patch("/api/orders/{id}/external-reference", draftOrderId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.externalReference").value("TEJ-OWNER"));
    }

    @Test
    void getShopAgents_asForeignSupplier_returns403() throws Exception {
        mockMvc.perform(get("/api/orders/shop-agents")
                        .param("shopId", shopId.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(foreignSupplierAdmin())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getShopAgents_asOwningSupplier_returns200() throws Exception {
        mockMvc.perform(get("/api/orders/shop-agents")
                        .param("shopId", shopId.toString())
                        .with(SecurityMockMvcRequestPostProcessors.authentication(supplierAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void createDispute_asForeignShop_returns403() throws Exception {
        UUID rejectedOrderId = createRejectedOrder();
        mockMvc.perform(post("/api/disputes")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(foreignShopAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(disputeBody(rejectedOrderId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createDispute_asOwningShop_returns201() throws Exception {
        UUID rejectedOrderId = createRejectedOrder();
        mockMvc.perform(post("/api/disputes")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(disputeBody(rejectedOrderId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.orderId").value(rejectedOrderId.toString()));
    }

    @Test
    void getDispute_asForeignShop_returns403() throws Exception {
        UUID rejectedOrderId = createRejectedOrder();
        String response = mockMvc.perform(post("/api/disputes")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(disputeBody(rejectedOrderId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String disputeId = objectMapper.readTree(response).get("id").asText();

        mockMvc.perform(get("/api/disputes/{id}", disputeId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(foreignShopAdmin())))
                .andExpect(status().isForbidden());
    }

    @Test
    void listOrders_asForeignShop_doesNotLeakOrders() throws Exception {
        mockMvc.perform(get("/api/orders")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(foreignShopAdmin())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }
}
