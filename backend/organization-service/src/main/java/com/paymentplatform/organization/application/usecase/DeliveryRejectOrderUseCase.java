package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.OrderEventRepository;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.ProductRepository;
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
public class DeliveryRejectOrderUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final ProductRepository products;
    private final OutboxEventStore outbox;

    public DeliveryRejectOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                                      OrderEventRepository events, ProductRepository products,
                                      OutboxEventStore outbox) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.products = products;
        this.outbox = outbox;
    }

    @Transactional
    public OrderResponse execute(UUID orderId, UUID actorUserId, String reason) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        String status = order.getStatus();
        if (!"READY_FOR_DELIVERY".equals(status) && !"IN_DELIVERY".equals(status)) {
            throw new ConflictException("La commande doit être en statut READY_FOR_DELIVERY ou IN_DELIVERY pour rejeter la livraison");
        }

        order.deliveryReject(reason);
        orders.save(order);

        List<OrderItem> items = orderItems.findByOrderId(orderId);

        events.save(OrderEvent.create(orderId, "ORDER_DELIVERY_REJECTED", actorUserId,
                reason != null ? reason : "Livraison rejetée"));
        outbox.append(new OrderEvents.OrderDeliveryRejectedEvent(UUID.randomUUID(), Instant.now(),
                orderId, order.getReference(),
                order.getShopId(), order.getSupplierId(), actorUserId, reason),
                String.valueOf(orderId));

        return OrderResponse.from(order, items);
    }
}
