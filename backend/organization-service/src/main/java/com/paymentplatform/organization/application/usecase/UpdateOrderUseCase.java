package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.application.dto.UpdateOrderRequest;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.*;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class UpdateOrderUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final ProductRepository products;

    public UpdateOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                              OrderEventRepository events, ProductRepository products) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.products = products;
    }

    @Transactional
    public OrderResponse execute(UUID orderId, UpdateOrderRequest request, UUID actorUserId) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new ConflictException("La commande doit contenir au moins un article");
        }

        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        if (!"DRAFT".equals(order.getStatus())) {
            throw new ConflictException("Seules les commandes en statut BROUILLON peuvent être modifiées");
        }

        List<OrderItem> oldItems = orderItems.findByOrderId(orderId);
        for (OrderItem oldItem : oldItems) {
            Product product = products.findByIdForUpdate(oldItem.getProductId())
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé : " + oldItem.getProductId()));
            product.setReservedQty(Math.max(0, product.getReservedQty() - oldItem.getQuantity()));
            products.save(product);
        }
        orderItems.deleteAll(oldItems);

        if (request.notes() != null) {
            order.setNotes(request.notes());
        }
        if (request.asapPayment() != null) {
            order.setAsapPayment(request.asapPayment());
        }

        List<OrderItem> newItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (OrderItemRequest itemReq : request.items()) {
            Product product = products.findById(itemReq.productId())
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé : " + itemReq.productId()));

            int availableQty = product.getQuantity() - product.getReservedQty();
            if (availableQty < itemReq.quantity()) {
                throw new ConflictException("Stock insuffisant pour le produit " + product.getName()
                        + " (disponible: " + availableQty + ", demandé: " + itemReq.quantity() + ")");
            }

            BigDecimal discount = itemReq.discount() != null ? itemReq.discount() : BigDecimal.ZERO;

            OrderItem item = new OrderItem();
            item.setOrderId(orderId);
            item.setProductId(product.getId());
            item.setProductRef(product.getSku());
            item.setProductName(product.getName());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(product.getUnitPrice());
            item.setDiscount(discount);
            item.setProductSnapshot(buildProductSnapshot(product));

            product.setReservedQty(product.getReservedQty() + itemReq.quantity());
            products.save(product);

            OrderItem savedItem = orderItems.save(item);
            newItems.add(savedItem);
            subtotal = subtotal.add(savedItem.getLineTotal());
        }

        order.setSubtotal(subtotal);
        order.recalculateTotals();
        orders.save(order);

        events.save(OrderEvent.create(orderId, "ORDER_UPDATED", actorUserId, "Commande modifiée"));

        return OrderResponse.from(order, newItems);
    }

    private String buildProductSnapshot(Product product) {
        String name = product.getName() != null ? product.getName() : "";
        String sku = product.getSku() != null ? product.getSku() : "";
        String price = product.getUnitPrice() != null ? product.getUnitPrice().toPlainString() : "0";
        return "{\"name\":\"" + escapeJson(name) + "\",\"sku\":\"" + escapeJson(sku)
                + "\",\"unitPrice\":" + price + "}";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
