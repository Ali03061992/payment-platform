package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.*;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
/**
 * Tests de OrderUseCaseH2Test.
 * Perimetre : cas d'usage/service OrderUseCase sur base H2.
 * Moyens : contexte SpringBootTest, profil "test" (H2).
 */

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderUseCaseH2Test {

    @Autowired private CreateOrganizationUseCase createOrg;
    @Autowired private SupplierShopRelationUseCase relationUseCase;
    @Autowired private CreateOrderUseCase createOrder;
    @Autowired private ConfirmOrderUseCase confirmOrder;
    @Autowired private PrepareOrderUseCase prepareOrder;
    @Autowired private DeliverOrderUseCase deliverOrder;
    @Autowired private AcceptDeliveryUseCase acceptDelivery;
    @Autowired private ConfirmDeliveryUseCase confirmDelivery;
    @Autowired private AutoAcceptDeliveriesUseCase autoAcceptDeliveries;
    @Autowired private AcceptOrderUseCase acceptOrder;
    @Autowired private CancelOrderUseCase cancelOrder;
    @Autowired private RejectOrderUseCase rejectOrder;
    @Autowired private DeliveryRejectOrderUseCase deliveryRejectOrder;
    @Autowired private UpdateOrderUseCase updateOrder;
    @Autowired private ProductRepository products;
    @Autowired private OrderRepository orderRepository;

    private UUID supplierId;
    private UUID shopId;
    private UUID productId;

    @BeforeEach
    void setUp() {
        var supplier = createOrg.execute(new CreateOrganizationRequest("Ord Supplier", "SUPPLIER"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        supplierId = supplier.id();
        var shop = createOrg.execute(new CreateOrganizationRequest("Ord Shop", "SHOP"), UUID.fromString("00000000-0000-0000-0000-000000000001"));
        shopId = shop.id();
        relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));

        Product product = new Product();
        product.setSupplierId(supplierId);
        product.setName("Test Product");
        product.setSku("TP-001");
        product.setUnitPrice(new BigDecimal("10.00"));
        product.setCurrency("TND");
        product.setQuantity(100);
        product.setMinQuantity(10);
        product.setReservedQty(0);
        Product saved = products.save(product);
        productId = saved.getId();
    }

    private OrderResponse createShopOrder() {
        var request = new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, null, "Test order",
                List.of(new OrderItemRequest(productId, 5, null)));
        return createOrder.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000010"), "SHOP");
    }

    private OrderResponse createSupplierOrder() {
        var request = new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, null, "Test order",
                List.of(new OrderItemRequest(productId, 5, null)));
        return createOrder.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000010"), "SUPPLIER");
    }

    @Test
    void createOrder_shop_createsDraft() {
        var response = createShopOrder();
        assertThat(response.id()).isNotNull();
        assertThat(response.status()).isEqualTo("DRAFT");
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void createOrder_supplier_autoConfirmsAndPrepares() {
        var response = createSupplierOrder();
        assertThat(response.status()).isEqualTo("PREPARING");
    }

    @Test
    void createOrder_emptyItems_throwsConflict() {
        var request = new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, null, null, List.of());
        assertThatThrownBy(() -> createOrder.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000010"), "SHOP"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createOrder_sameSupplierAndShop_throws() {
        var request = new CreateOrderRequest(supplierId, supplierId, false, null, "TND", null, null, null,
                List.of(new OrderItemRequest(productId, 5, null)));
        assertThatThrownBy(() -> createOrder.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000010"), "SHOP"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createOrder_unknownProduct_throwsNotFound() {
        var request = new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, null, null,
                List.of(new OrderItemRequest(UUID.fromString("00000000-0000-0000-0000-000000000999"), 5, null)));
        assertThatThrownBy(() -> createOrder.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000010"), "SHOP"))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createOrder_insufficientStock_throwsConflict() {
        var request = new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, null, null,
                List.of(new OrderItemRequest(productId, 999, null)));
        assertThatThrownBy(() -> createOrder.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000010"), "SHOP"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Stock insuffisant");
    }

    @Test
    void fullOrderLifecycle_shopOrder() {
        var order = createShopOrder();
        String orderId = order.id().toString();

        // Confirm
        var confirmed = confirmOrder.execute(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(confirmed.status()).isEqualTo("CONFIRMED");

        // Prepare
        var prepared = prepareOrder.execute(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(prepared.status()).isEqualTo("PREPARING");

        // Ready for delivery
        var ready = prepareOrder.readyForDelivery(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(ready.status()).isEqualTo("READY_FOR_DELIVERY");
    }

    @Test
    void cancelOrder_fromDraft_succeeds() {
        var order = createShopOrder();
        var cancelled = cancelOrder.execute(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelOrder_fromConfirmed_succeeds() {
        var order = createShopOrder();
        confirmOrder.execute(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        var cancelled = cancelOrder.execute(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }

    @Test
    void cancelOrder_fromPreparing_succeeds() {
        var order = createShopOrder();
        confirmOrder.execute(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        prepareOrder.execute(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        var cancelled = cancelOrder.execute(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }

    @Test
    void rejectOrder_notDelivered_throwsConflict() {
        var order = createShopOrder();
        assertThatThrownBy(() -> rejectOrder.execute(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"), "Produit non conforme"))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void deliveryReject_fromReadyForDelivery_cancelsOrderAndReleasesStock() {
        var order = createShopOrder();
        var actor = UUID.fromString("00000000-0000-0000-0000-000000000010");
        confirmOrder.execute(order.id(), actor);
        prepareOrder.execute(order.id(), actor);
        prepareOrder.readyForDelivery(order.id(), actor);

        var rejected = deliveryRejectOrder.execute(order.id(), actor, "Client injoignable");

        assertThat(rejected.status()).isEqualTo("CANCELLED");
        assertThat(rejected.deliveryRejectionReason()).isEqualTo("Client injoignable");
        Product product = products.findById(productId).orElseThrow();
        assertThat(product.getReservedQty()).isEqualTo(0);
    }

    @Test
    void acceptDelivery_rejected_cancelsOrder() {
        var order = createShopOrder();
        var actor = UUID.fromString("00000000-0000-0000-0000-000000000010");
        confirmOrder.execute(order.id(), actor);
        prepareOrder.execute(order.id(), actor);
        prepareOrder.readyForDelivery(order.id(), actor);

        var rejected = acceptDelivery.execute(order.id(), false, "Véhicule en panne", actor);

        assertThat(rejected.status()).isEqualTo("CANCELLED");
        assertThat(rejected.deliveryRejectionReason()).isEqualTo("Véhicule en panne");
    }

    @Test
    void fullDeliveryFlow_deliver_setsDelivered() {
        var order = createShopOrder();
        var actor = UUID.fromString("00000000-0000-0000-0000-000000000010");
        var agent = UUID.fromString("00000000-0000-0000-0000-000000000004");
        confirmOrder.execute(order.id(), actor);
        prepareOrder.execute(order.id(), actor);
        prepareOrder.readyForDelivery(order.id(), actor);
        var entity = orderRepository.findById(order.id()).orElseThrow();
        entity.assignDeliveryAgent(agent);
        orderRepository.save(entity);
        acceptDelivery.execute(order.id(), true, null, actor);
        confirmDelivery.execute(order.id(), java.time.LocalDate.now().plusDays(1), actor);

        var delivered = deliverOrder.execute(order.id(), actor, actor);

        assertThat(delivered.status()).isEqualTo("DELIVERED");
    }

    @Test
    void autoAccept_staleDelivered_acceptsOrder() throws Exception {
        var order = createShopOrder();
        var actor = UUID.fromString("00000000-0000-0000-0000-000000000010");
        var agent = UUID.fromString("00000000-0000-0000-0000-000000000004");
        confirmOrder.execute(order.id(), actor);
        prepareOrder.execute(order.id(), actor);
        prepareOrder.readyForDelivery(order.id(), actor);
        var entity = orderRepository.findById(order.id()).orElseThrow();
        entity.assignDeliveryAgent(agent);
        orderRepository.save(entity);
        acceptDelivery.execute(order.id(), true, null, actor);
        confirmDelivery.execute(order.id(), java.time.LocalDate.now().plusDays(1), actor);
        deliverOrder.execute(order.id(), actor, actor);

        backdateDeliveredAt(order.id(), 16);

        int accepted = autoAcceptDeliveries.execute();

        assertThat(accepted).isEqualTo(1);
        var acceptedOrder = orderRepository.findById(order.id()).orElseThrow();
        assertThat(acceptedOrder.getStatus()).isEqualTo("ACCEPTED");
    }

    @Test
    void autoAccept_recentDelivered_keepsDelivered() {
        var order = createShopOrder();
        var actor = UUID.fromString("00000000-0000-0000-0000-000000000010");
        var agent = UUID.fromString("00000000-0000-0000-0000-000000000004");
        confirmOrder.execute(order.id(), actor);
        prepareOrder.execute(order.id(), actor);
        prepareOrder.readyForDelivery(order.id(), actor);
        var entity = orderRepository.findById(order.id()).orElseThrow();
        entity.assignDeliveryAgent(agent);
        orderRepository.save(entity);
        acceptDelivery.execute(order.id(), true, null, actor);
        confirmDelivery.execute(order.id(), java.time.LocalDate.now().plusDays(1), actor);
        deliverOrder.execute(order.id(), actor, actor);

        int accepted = autoAcceptDeliveries.execute();

        assertThat(accepted).isEqualTo(0);
        var stillDelivered = orderRepository.findById(order.id()).orElseThrow();
        assertThat(stillDelivered.getStatus()).isEqualTo("DELIVERED");
    }

    private void backdateDeliveredAt(UUID orderId, long minutesAgo) throws Exception {
        var entity = orderRepository.findById(orderId).orElseThrow();
        var field = com.paymentplatform.organization.domain.model.Order.class.getDeclaredField("deliveredAt");
        field.setAccessible(true);
        field.set(entity, java.time.Instant.now().minus(minutesAgo, java.time.temporal.ChronoUnit.MINUTES));
        orderRepository.saveAndFlush(entity);
    }

    @Test
    void deliveryReject_notReadyForDelivery_throwsConflict() {
        var order = createShopOrder();
        assertThatThrownBy(() -> deliveryRejectOrder.execute(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"), "Bad delivery"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("READY_FOR_DELIVERY");
    }

    @Test
    void confirmOrder_notDraft_throwsConflict() {
        var order = createShopOrder();
        confirmOrder.execute(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThatThrownBy(() -> confirmOrder.execute(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000010")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void createOrder_withNotes_succeeds() {
        var request = new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, null, "Special instructions",
                List.of(new OrderItemRequest(productId, 3, null)));
        var response = createOrder.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000010"), "SHOP");
        assertThat(response.status()).isEqualTo("DRAFT");
    }

    @Test
    void createOrder_withDiscount_succeeds() {
        var request = new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, null, null,
                List.of(new OrderItemRequest(productId, 3, new BigDecimal("2.00"))));
        var response = createOrder.execute(request, UUID.fromString("00000000-0000-0000-0000-000000000010"), "SHOP");
        assertThat(response.items()).hasSize(1);
    }

    @Test
    void acceptOrder_notDelivered_throwsConflict() {
        var order = createShopOrder();
        assertThatThrownBy(() -> acceptOrder.execute(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000015")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("DELIVERED");
    }

    @Test
    void updateOrder_draft_succeeds() {
        var order = createShopOrder();
        assertThat(order.status()).isEqualTo("DRAFT");

        var updateRequest = new UpdateOrderRequest("Updated notes", false,
                List.of(new OrderItemRequest(productId, 10, null)));
        var updated = updateOrder.execute(order.id(), updateRequest, UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(updated.status()).isEqualTo("DRAFT");
        assertThat(updated.notes()).isEqualTo("Updated notes");
        assertThat(updated.items()).hasSize(1);
        assertThat(updated.items().get(0).quantity()).isEqualTo(10);
    }

    @Test
    void updateOrder_notDraft_throwsConflict() {
        var order = createShopOrder();
        confirmOrder.execute(order.id(), UUID.fromString("00000000-0000-0000-0000-000000000010"));

        var updateRequest = new UpdateOrderRequest("Notes", false,
                List.of(new OrderItemRequest(productId, 3, null)));
        assertThatThrownBy(() -> updateOrder.execute(order.id(), updateRequest, UUID.fromString("00000000-0000-0000-0000-000000000010")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("BROUILLON");
    }

    @Test
    void updateOrder_emptyItems_throwsConflict() {
        var order = createShopOrder();
        var updateRequest = new UpdateOrderRequest("Notes", false, List.of());
        assertThatThrownBy(() -> updateOrder.execute(order.id(), updateRequest, UUID.fromString("00000000-0000-0000-0000-000000000010")))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void updateOrder_unknownProduct_throwsNotFound() {
        var order = createShopOrder();
        var updateRequest = new UpdateOrderRequest(null, null,
                List.of(new OrderItemRequest(UUID.fromString("00000000-0000-0000-0000-000000000999"), 5, null)));
        assertThatThrownBy(() -> updateOrder.execute(order.id(), updateRequest, UUID.fromString("00000000-0000-0000-0000-000000000010")))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateOrder_insufficientStock_throwsConflict() {
        var order = createShopOrder();
        var updateRequest = new UpdateOrderRequest(null, null,
                List.of(new OrderItemRequest(productId, 999, null)));
        assertThatThrownBy(() -> updateOrder.execute(order.id(), updateRequest, UUID.fromString("00000000-0000-0000-0000-000000000010")))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Stock insuffisant");
    }

    @Test
    void updateOrder_releasesOldReservedQty() {
        var order = createShopOrder();
        var product = products.findById(productId).orElseThrow();
        int reservedBefore = product.getReservedQty();
        assertThat(reservedBefore).isGreaterThanOrEqualTo(5);

        var updateRequest = new UpdateOrderRequest(null, null,
                List.of(new OrderItemRequest(productId, 2, null)));
        updateOrder.execute(order.id(), updateRequest, UUID.fromString("00000000-0000-0000-0000-000000000010"));

        var updatedProduct = products.findById(productId).orElseThrow();
        assertThat(updatedProduct.getReservedQty()).isEqualTo(reservedBefore - 5 + 2);
    }

    @Test
    void updateOrder_nonExistent_throwsNotFound() {
        var updateRequest = new UpdateOrderRequest("Notes", false,
                List.of(new OrderItemRequest(productId, 1, null)));
        assertThatThrownBy(() -> updateOrder.execute(UUID.fromString("00000000-0000-0000-0000-000000099999"), updateRequest, UUID.fromString("00000000-0000-0000-0000-000000000010")))
                .isInstanceOf(NotFoundException.class);
    }
}
