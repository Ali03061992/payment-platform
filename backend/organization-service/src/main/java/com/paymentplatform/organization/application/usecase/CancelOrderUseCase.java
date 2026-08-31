package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderStatus;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.*;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CancelOrderUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final ProductRepository products;

    public CancelOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                              OrderEventRepository events, ProductRepository products) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.products = products;
    }

    @Transactional
    public OrderResponse execute(Long orderId, long actorUserId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        if (!OrderStatus.valueOf(order.getStatus()).canCancel()) {
            throw new ConflictException("La commande ne peut pas être annulée dans le statut actuel : " + order.getStatus());
        }

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        for (OrderItem item : items) {
            Product product = products.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé : " + item.getProductId()));
            product.setReservedQty(Math.max(0, product.getReservedQty() - item.getQuantity()));
            products.save(product);
        }

        order.cancel();
        orders.save(order);

        events.save(OrderEvent.create(orderId, "ORDER_CANCELLED", actorUserId, null));

        return OrderResponse.from(order, items);
    }
}
