package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.application.security.TenantAccess;
import com.paymentplatform.organization.domain.model.*;
import com.paymentplatform.organization.domain.repository.OrderEventRepository;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.ProductRepository;
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
public class CancelOrderUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final ProductRepository products;
    private final OutboxEventStore outbox;
    private final BalanceUseCase balanceUseCase;
    private final PaymentClient paymentClient;

    public CancelOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                              OrderEventRepository events, ProductRepository products,
                              OutboxEventStore outbox, BalanceUseCase balanceUseCase,
                              PaymentClient paymentClient) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.products = products;
        this.outbox = outbox;
        this.balanceUseCase = balanceUseCase;
        this.paymentClient = paymentClient;
    }

    @Transactional
    public OrderResponse execute(UUID orderId, ActorContext actor) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        TenantAccess.requireParticipant(actor, order.getShopId(), order.getSupplierId(), null);
        return cancel(order, actor.userId());
    }

    @Deprecated
    @Transactional
    public OrderResponse execute(UUID orderId, UUID actorUserId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        return cancel(order, actorUserId);
    }

    private OrderResponse cancel(Order order, UUID actorUserId) {
        UUID orderId = order.getId();
        if (!OrderStatus.valueOf(order.getStatus()).canCancel()) {
            throw new ConflictException("La commande ne peut pas être annulée dans le statut actuel : " + order.getStatus());
        }
        boolean alreadyCredited = "CONFIRMED".equals(order.getStatus())
                || "PREPARING".equals(order.getStatus());

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        for (OrderItem item : items) {
            Product product = products.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé : " + item.getProductId()));
            product.setReservedQty(Math.max(0, product.getReservedQty() - item.getQuantity()));
            products.save(product);
        }

        order.cancel();
        orders.save(order);

        if (alreadyCredited) {
            balanceUseCase.reverseOrderCredit(order.getSupplierId(), order.getShopId(),
                    order.getTotal(), orderId, "Annulation de la commande " + order.getReference(),
                    actorUserId);
            paymentClient.cancelAutoPayment(orderId, actorUserId);
        }

        events.save(OrderEvent.create(orderId, "ORDER_CANCELLED", actorUserId, null));
        outbox.append(new OrderEvents.OrderCancelledEvent(UUID.randomUUID(), Instant.now(),
                orderId, order.getReference(),
                order.getShopId(), order.getSupplierId(), actorUserId),
                String.valueOf(orderId));

        return OrderResponse.from(order, items);
    }
}
