package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.repository.OrderEventRepository;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AcceptDeliveryUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final OutboxEventStore outbox;

    public AcceptDeliveryUseCase(OrderRepository orders, OrderItemRepository orderItems,
                                 OrderEventRepository events, OutboxEventStore outbox) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.outbox = outbox;
    }

    @Transactional
    public OrderResponse execute(UUID orderId, boolean accepted, String reason, UUID actorUserId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        if (!"READY_FOR_DELIVERY".equals(order.getStatus())) {
            throw new ConflictException("La commande doit être en statut READY_FOR_DELIVERY pour accepter ou rejeter la livraison");
        }

        if (accepted) {
            order.acceptDelivery();
            events.save(OrderEvent.create(orderId, "ORDER_DELIVERY_ACCEPTED", actorUserId, null));
            outbox.append(new OrderEvents.OrderDeliveryAcceptedEvent(UUID.randomUUID(), Instant.now(),
                    orderId, order.getReference(),
                    order.getShopId(), order.getSupplierId(), actorUserId),
                    String.valueOf(orderId));
        } else {
            order.deliveryReject(reason);
            events.save(OrderEvent.create(orderId, "ORDER_DELIVERY_REJECTED", actorUserId,
                    reason != null ? reason : "Livraison rejetée par le livreur"));
            outbox.append(new OrderEvents.OrderDeliveryRejectedEvent(UUID.randomUUID(), Instant.now(),
                    orderId, order.getReference(),
                    order.getShopId(), order.getSupplierId(), actorUserId, reason),
                    String.valueOf(orderId));
        }

        orders.save(order);
        List<OrderItem> items = orderItems.findByOrderId(orderId);
        return OrderResponse.from(order, items);
    }
}
