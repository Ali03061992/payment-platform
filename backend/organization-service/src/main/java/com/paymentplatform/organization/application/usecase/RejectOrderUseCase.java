package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.application.security.TenantAccess;
import com.paymentplatform.organization.domain.model.*;
import com.paymentplatform.organization.domain.repository.*;
import com.paymentplatform.organization.infrastructure.http.PaymentClient;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.security.ActorContext;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RejectOrderUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final ProductRepository products;
    private final StockMovementRepository stockMovements;
    private final OutboxEventStore outbox;
    private final BalanceUseCase balanceUseCase;
    private final PaymentClient paymentClient;

    public RejectOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                              OrderEventRepository events, ProductRepository products,
                              StockMovementRepository stockMovements, OutboxEventStore outbox,
                              BalanceUseCase balanceUseCase, PaymentClient paymentClient) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.products = products;
        this.stockMovements = stockMovements;
        this.outbox = outbox;
        this.balanceUseCase = balanceUseCase;
        this.paymentClient = paymentClient;
    }

    @Transactional
    public OrderResponse execute(UUID orderId, ActorContext actor, String returnReason) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        TenantAccess.requireShop(actor, order.getShopId());
        return reject(order, actor.userId(), returnReason);
    }

    @Deprecated
    @Transactional
    public OrderResponse execute(UUID orderId, UUID actorUserId, String returnReason) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        return reject(order, actorUserId, returnReason);
    }

    private OrderResponse reject(Order order, UUID actorUserId, String returnReason) {
        UUID orderId = order.getId();
        if (!"DELIVERED".equals(order.getStatus())) {
            throw new ConflictException("La commande doit être en statut DELIVERED pour être rejetée");
        }

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        for (OrderItem item : items) {
            Product product = products.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé : " + item.getProductId()));
            product.setReservedQty(Math.max(0, product.getReservedQty() - item.getQuantity()));
            product.setQuantity(product.getQuantity() + item.getQuantity());
            products.save(product);

            StockMovement movement = new StockMovement();
            movement.setProductId(item.getProductId());
            movement.setSupplierId(order.getSupplierId());
            movement.setType("RETURN");
            movement.setQuantity(item.getQuantity());
            movement.setReference(order.getReference());
            movement.setNotes("Retour automatique suite au rejet de la commande " + order.getReference());
            stockMovements.save(movement);
        }

        order.setReturnReason(returnReason);
        order.reject();
        orders.save(order);

        balanceUseCase.reverseOrderCredit(order.getSupplierId(), order.getShopId(),
                order.getTotal(), orderId, "Rejet de la commande " + order.getReference(),
                actorUserId);
        paymentClient.cancelAutoPayment(orderId, actorUserId);

        events.save(OrderEvent.create(orderId, "ORDER_REJECTED", actorUserId,
                returnReason != null ? "Commande rejetée : " + returnReason : "Commande rejetée par la boutique"));
        outbox.append(new OrderEvents.OrderRejectedEvent(UUID.randomUUID(), Instant.now(),
                orderId, order.getReference(),
                order.getShopId(), order.getSupplierId(), actorUserId),
                String.valueOf(orderId));

        return OrderResponse.from(order, items);
    }
}
