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
public class DeliveryRejectOrderUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;

    public DeliveryRejectOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                                      OrderEventRepository events) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
    }

    @Transactional
    public OrderResponse execute(Long orderId, long actorUserId, String reason) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        if (!"IN_DELIVERY".equals(order.getStatus())) {
            throw new ConflictException("La commande doit être en statut IN_DELIVERY pour rejeter la livraison");
        }

        order.deliveryReject();
        orders.save(order);

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        events.save(OrderEvent.create(orderId, "ORDER_DELIVERY_REJECTED", actorUserId,
                reason != null ? reason : "Livraison rejetée"));

        return OrderResponse.from(order, items);
    }
}
