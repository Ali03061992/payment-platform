package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.repository.*;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class DeliverOrderUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;

    public DeliverOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                               OrderEventRepository events) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
    }

    @Transactional
    public OrderResponse execute(Long orderId, Long receivedBy, long actorUserId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        order.deliver(receivedBy);
        orders.save(order);

        events.save(OrderEvent.create(orderId, "ORDER_DELIVERED", actorUserId,
                "Reçu par: " + receivedBy));

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        return OrderResponse.from(order, items);
    }
}
