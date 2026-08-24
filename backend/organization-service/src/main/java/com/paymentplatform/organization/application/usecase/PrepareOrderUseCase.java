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
public class PrepareOrderUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;

    public PrepareOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                               OrderEventRepository events) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
    }

    @Transactional
    public OrderResponse execute(Long orderId, long actorUserId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        order.prepare();
        orders.save(order);

        events.save(OrderEvent.create(orderId, "ORDER_PREPARING", actorUserId, null));

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        return OrderResponse.from(order, items);
    }

    @Transactional
    public OrderResponse readyForDelivery(Long orderId, long actorUserId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        order.readyForDelivery();
        orders.save(order);

        events.save(OrderEvent.create(orderId, "ORDER_READY_FOR_DELIVERY", actorUserId, null));

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        return OrderResponse.from(order, items);
    }
}
