package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.repository.*;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RejectOrderUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;

    public RejectOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                              OrderEventRepository events) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
    }

    @Transactional
    public OrderResponse execute(Long orderId, long actorUserId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        if (!"DELIVERED".equals(order.getStatus())) {
            throw new ConflictException("La commande doit être en statut DELIVERED pour être rejetée");
        }

        order.reject();
        orders.save(order);

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        events.save(OrderEvent.create(orderId, "ORDER_REJECTED", actorUserId, "Commande rejetée par la boutique"));

        return OrderResponse.from(order, items);
    }
}
