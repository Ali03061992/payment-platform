package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.application.dto.UpdateOrderRequest;
import com.paymentplatform.organization.application.security.TenantAccess;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderFiscalizer;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.OrderEventRepository;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.security.ActorContext;
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
    private final OrganizationRepository organizations;

    public UpdateOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                              OrderEventRepository events, ProductRepository products,
                              OrganizationRepository organizations) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.products = products;
        this.organizations = organizations;
    }

    @Transactional
    public OrderResponse execute(UUID orderId, UpdateOrderRequest request, ActorContext actor) {
        return execute(orderId, request, actor, true);
    }

    @Deprecated
    @Transactional
    public OrderResponse execute(UUID orderId, UpdateOrderRequest request, UUID actorUserId) {
        return execute(orderId, request, new ActorContext(actorUserId, null, List.of()), false);
    }

    private OrderResponse execute(UUID orderId, UpdateOrderRequest request, ActorContext actor,
                                  boolean enforceTenant) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new ConflictException("La commande doit contenir au moins un article");
        }

        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        if (enforceTenant) {
            TenantAccess.requireParticipant(actor, order.getShopId(), order.getSupplierId(), null);
        }
        UUID actorUserId = actor != null ? actor.userId() : null;

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

        Organization shop = organizations.findById(
                        new com.paymentplatform.organization.domain.valueobject.OrganizationId(order.getShopId()))
                .orElseThrow(() -> new NotFoundException("Boutique non trouvée : " + order.getShopId()));

        List<OrderItem> newItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.items()) {
            Product product = products.findById(itemReq.productId())
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé : " + itemReq.productId()));
            if (!order.getSupplierId().equals(product.getSupplierId())) {
                throw new ForbiddenException("Produit hors catalogue du fournisseur");
            }

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
            CreateOrderUseCase.snapshotFiscalFields(item, product);
            item.setProductSnapshot(buildProductSnapshot(product));

            product.setReservedQty(product.getReservedQty() + itemReq.quantity());
            products.save(product);

            newItems.add(item);
        }

        OrderFiscalizer.apply(order, newItems, shop);
        orders.save(order);
        List<OrderItem> savedItems = new ArrayList<>();
        for (OrderItem item : newItems) {
            savedItems.add(orderItems.save(item));
        }
        newItems = savedItems;

        events.save(OrderEvent.create(orderId, "ORDER_UPDATED", actorUserId, "Commande modifiée"));

        return OrderResponse.from(order, newItems);
    }

    private String buildProductSnapshot(Product product) {
        String name = product.getName() != null ? product.getName() : "";
        String sku = product.getSku() != null ? product.getSku() : "";
        String price = product.getUnitPrice() != null ? product.getUnitPrice().toPlainString() : "0";
        String vat = product.getVatRate() != null ? product.getVatRate().toPlainString() : "19";
        boolean fodec = product.isFodecApplicable();
        return "{\"name\":\"" + escapeJson(name) + "\",\"sku\":\"" + escapeJson(sku)
                + "\",\"unitPrice\":" + price + ",\"vatRate\":" + vat + ",\"fodec\":" + fodec
                + ",\"priceTtc\":" + product.isPriceTtc() + "}";
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

