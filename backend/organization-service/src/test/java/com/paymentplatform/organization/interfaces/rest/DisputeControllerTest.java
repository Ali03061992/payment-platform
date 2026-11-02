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
import org.springframework.mock.web.MockMultipartFile;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
    void createTwiceOnSameOrder_returns409() throws Exception {
        var shopAdmin = auth(UUID.fromString("00000000-0000-0000-0000-000000000026"), "shop.admin", List.of("SHOP_ADMIN"), shopId);
        mockMvc.perform(post("/api/disputes")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(rejectedOrderId, "Premier litige")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/disputes")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(shopAdmin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(rejectedOrderId, "Deuxieme litige")))
                .andExpect(status().isConflict());
    }

    @Test
    void eligibleForDispute_asShopAgent_returnsOnlyEligibleOrders() throws Exception {
        createOrder.execute(
                new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, "Commande brouillon",
                        List.of(new OrderItemRequest(productId, 1, null))),
                UUID.fromString("00000000-0000-0000-0000-000000000010"), "SHOP");
        var agent = auth(UUID.fromString("00000000-0000-0000-0000-000000000027"), "shop.agent", List.of("SHOP_AGENT"), shopId);
        mockMvc.perform(get("/api/orders/eligible-for-dispute")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].id").value(rejectedOrderId.toString()));
    }

    @Test
    void eligibleForDispute_asForeignShop_returnsEmpty() throws Exception {
        var foreignAgent = auth(UUID.fromString("00000000-0000-0000-0000-000000000028"), "foreign.agent", List.of("SHOP_AGENT"), foreignShopId);
        mockMvc.perform(get("/api/orders/eligible-for-dispute")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(foreignAgent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test
    void list_asSupplierAgent_returnsOk() throws Exception {
        var agent = auth(UUID.fromString("00000000-0000-0000-0000-000000000025"), "supplier.agent", List.of("SUPPLIER_AGENT"), supplierId);
        mockMvc.perform(get("/api/disputes")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    private String body(UUID orderId, String reason, String priority) throws Exception {
        return objectMapper.writeValueAsString(
                new com.paymentplatform.organization.application.dto.CreateDisputeRequest(orderId, reason, priority));
    }

    private String openDispute(UUID userId, String role, UUID orgId, String priority) throws Exception {
        var principal = auth(userId, role.toLowerCase(), List.of(role), orgId);
        var result = mockMvc.perform(post("/api/disputes")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(principal))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(rejectedOrderId, "Litige " + priority, priority)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void create_withUrgentPriority_setsSlaAndHistory() throws Exception {
        String disputeId = openDispute(UUID.fromString("00000000-0000-0000-0000-000000000031"),
                "SHOP_ADMIN", shopId, "URGENT");
        var admin = auth(UUID.fromString("00000000-0000-0000-0000-000000000031"), "shop.admin", List.of("SHOP_ADMIN"), shopId);
        mockMvc.perform(get("/api/disputes/{id}", disputeId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("URGENT"))
                .andExpect(jsonPath("$.dueAt").exists())
                .andExpect(jsonPath("$.history.length()").value(1))
                .andExpect(jsonPath("$.history[0].toStatus").value("OPEN"));
    }

    @Test
    void create_withInvalidPriority_returns409() throws Exception {
        var admin = auth(UUID.fromString("00000000-0000-0000-0000-000000000032"), "shop.admin", List.of("SHOP_ADMIN"), shopId);
        mockMvc.perform(post("/api/disputes")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body(rejectedOrderId, "Priorite inconnue", "CRITICAL")))
                .andExpect(status().isConflict());
    }

    @Test
    void transition_openToInProgress_byShopAdmin_returnsStatus() throws Exception {
        String disputeId = openDispute(UUID.fromString("00000000-0000-0000-0000-000000000033"),
                "SHOP_ADMIN", shopId, "HIGH");
        var admin = auth(UUID.fromString("00000000-0000-0000-0000-000000000033"), "shop.admin", List.of("SHOP_ADMIN"), shopId);
        mockMvc.perform(post("/api/disputes/{id}/transition", disputeId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
        mockMvc.perform(get("/api/disputes/{id}", disputeId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(admin)))
                .andExpect(jsonPath("$.history.length()").value(2))
                .andExpect(jsonPath("$.history[1].toStatus").value("IN_PROGRESS"));
    }

    @Test
    void priorityAndNotes_byAdmin_areApplied() throws Exception {
        String disputeId = openDispute(UUID.fromString("00000000-0000-0000-0000-000000000034"),
                "SUPPLIER_ADMIN", supplierId, "NORMAL");
        var admin = auth(UUID.fromString("00000000-0000-0000-0000-000000000034"), "supplier.admin", List.of("SUPPLIER_ADMIN"), supplierId);
        mockMvc.perform(patch("/api/disputes/{id}/priority", disputeId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"URGENT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority").value("URGENT"));
        mockMvc.perform(post("/api/disputes/{id}/notes", disputeId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Relance fournisseur effectuée\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/disputes/{id}", disputeId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(admin)))
                .andExpect(jsonPath("$.notes.length()").value(1))
                .andExpect(jsonPath("$.notes[0].content").value("Relance fournisseur effectuée"));
    }

    @Test
    void priority_byAgent_returns403() throws Exception {
        String disputeId = openDispute(UUID.fromString("00000000-0000-0000-0000-000000000035"),
                "SHOP_ADMIN", shopId, "NORMAL");
        var agent = auth(UUID.fromString("00000000-0000-0000-0000-000000000036"), "shop.agent", List.of("SHOP_AGENT"), shopId);
        mockMvc.perform(patch("/api/disputes/{id}/priority", disputeId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(agent))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":\"HIGH\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void assign_withoutBody_assignsCurrentAdmin() throws Exception {
        String disputeId = openDispute(UUID.fromString("00000000-0000-0000-0000-000000000037"),
                "SHOP_ADMIN", shopId, "NORMAL");
        var admin = auth(UUID.fromString("00000000-0000-0000-0000-000000000037"), "shop.admin", List.of("SHOP_ADMIN"), shopId);
        mockMvc.perform(post("/api/disputes/{id}/assign", disputeId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTo").value("00000000-0000-0000-0000-000000000037"));
    }

    @Test
    void uploadAndDownloadAttachment_returnsFile() throws Exception {
        String disputeId = openDispute(UUID.fromString("00000000-0000-0000-0000-000000000038"),
                "SHOP_ADMIN", shopId, "NORMAL");
        var admin = auth(UUID.fromString("00000000-0000-0000-0000-000000000038"), "shop.admin", List.of("SHOP_ADMIN"), shopId);
        MockMultipartFile file = new MockMultipartFile("file", "preuve.pdf", "application/pdf", "contenu".getBytes());
        var upload = mockMvc.perform(multipart("/api/disputes/{id}/attachments", disputeId)
                        .file(file)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.attachments.length()").value(1))
                .andReturn();
        String attachmentId = objectMapper.readTree(upload.getResponse().getContentAsString())
                .get("attachments").get(0).get("id").asText();
        mockMvc.perform(get("/api/disputes/{id}/attachments/{attachmentId}", disputeId, attachmentId)
                        .with(SecurityMockMvcRequestPostProcessors.authentication(admin)))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("preuve.pdf")))
                .andExpect(content().bytes("contenu".getBytes()));
    }

    @Test
    void paged_asSupplierAgent_returnsScopedPage() throws Exception {
        openDispute(UUID.fromString("00000000-0000-0000-0000-000000000039"),
                "SUPPLIER_ADMIN", supplierId, "HIGH");
        var agent = auth(UUID.fromString("00000000-0000-0000-0000-000000000039"), "supplier.agent", List.of("SUPPLIER_AGENT"), supplierId);
        mockMvc.perform(get("/api/disputes/paged")
                        .param("priority", "HIGH")
                        .param("size", "10")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(agent)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].priority").value("HIGH"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }
}
