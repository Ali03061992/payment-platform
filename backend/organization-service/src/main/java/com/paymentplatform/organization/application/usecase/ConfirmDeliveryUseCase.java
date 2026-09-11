package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.repository.*;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class ConfirmDeliveryUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final OutboxEventStore outbox;

    public ConfirmDeliveryUseCase(OrderRepository orders, OrderItemRepository orderItems,
                                  OrderEventRepository events, OutboxEventStore outbox) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.outbox = outbox;
    }

    @Transactional
    public OrderResponse execute(UUID orderId, LocalDate confirmedDate, UUID actorUserId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        if (order.getDeliveryAgentId() == null) {
            throw new ConflictException("Aucun agent de livraison assigné à cette commande");
        }

        if (!"READY_FOR_DELIVERY".equals(order.getStatus())) {
            throw new ConflictException("La commande doit être en statut READY_FOR_DELIVERY pour confirmer la livraison");
        }

        order.confirmDelivery(confirmedDate);
        orders.save(order);

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        events.save(OrderEvent.create(orderId, "ORDER_DELIVERY_CONFIRMED", actorUserId,
                "Date de livraison confirmée: " + confirmedDate));
        outbox.append(new OrderEvents.OrderDeliveryConfirmedEvent(UUID.randomUUID(), Instant.now(),
                orderId, order.getReference(),
                order.getShopId(), order.getSupplierId(),
                actorUserId, confirmedDate.toString()),
                String.valueOf(orderId));

        return OrderResponse.from(order, items);
    }
}
