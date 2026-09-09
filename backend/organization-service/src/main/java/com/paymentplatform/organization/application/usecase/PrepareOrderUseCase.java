package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.repository.*;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PrepareOrderUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final OutboxEventStore outbox;

    public PrepareOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                               OrderEventRepository events, OutboxEventStore outbox) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.outbox = outbox;
    }

    @Transactional
    public OrderResponse execute(UUID orderId, UUID actorUserId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        order.prepare();
        orders.save(order);

        events.save(OrderEvent.create(orderId, "ORDER_PREPARING", actorUserId, null));
        outbox.append(new OrderEvents.OrderPreparingEvent(UUID.randomUUID(), Instant.now(),
                orderId, order.getReference(),
                order.getShopId(), order.getSupplierId(), actorUserId),
                String.valueOf(orderId));

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        return OrderResponse.from(order, items);
    }

    @Transactional
    public OrderResponse readyForDelivery(UUID orderId, UUID actorUserId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        order.readyForDelivery();
        orders.save(order);

        events.save(OrderEvent.create(orderId, "ORDER_READY_FOR_DELIVERY", actorUserId, null));
        outbox.append(new OrderEvents.OrderReadyForDeliveryEvent(UUID.randomUUID(), Instant.now(),
                orderId, order.getReference(),
                order.getShopId(), order.getSupplierId(), actorUserId),
                String.valueOf(orderId));

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        return OrderResponse.from(order, items);
    }
}
