package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.*;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConfirmOrderUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final ProductRepository products;

    public ConfirmOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
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

        if (!"DRAFT".equals(order.getStatus())) {
            throw new ConflictException("La commande doit être en statut DRAFT pour être confirmée");
        }

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        for (OrderItem item : items) {
            Product product = products.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé : " + item.getProductId()));
            int availableQty = product.getQuantity() - product.getReservedQty();
            if (availableQty < item.getQuantity()) {
                throw new ConflictException("Stock insuffisant pour le produit " + product.getName()
                        + " (disponible: " + availableQty + ", demandé: " + item.getQuantity() + ")");
            }
        }

        order.confirm();
        orders.save(order);

        events.save(OrderEvent.create(orderId, "ORDER_CONFIRMED", actorUserId, null));

        return OrderResponse.from(order, items);
    }
}
