package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.*;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class CreateOrderUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final ProductRepository products;
    private final OrganizationRepository organizations;
    private final SupplierShopRelationRepository relations;
    private final OutboxEventStore outbox;
    private final BalanceUseCase balanceUseCase;

    public CreateOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                              OrderEventRepository events, ProductRepository products,
                              OrganizationRepository organizations,
                              SupplierShopRelationRepository relations,
                              OutboxEventStore outbox, BalanceUseCase balanceUseCase) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.products = products;
        this.organizations = organizations;
        this.relations = relations;
        this.outbox = outbox;
        this.balanceUseCase = balanceUseCase;
    }

    @Transactional
    public OrderResponse execute(CreateOrderRequest request, UUID actorUserId, String actorRole) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new ConflictException("La commande doit contenir au moins un article");
        }
        if (request.supplierId().equals(request.shopId())) {
            throw new ConflictException("Le fournisseur et la boutique doivent être différents");
        }
        validateOrganizations(request.supplierId(), request.shopId());
        validateRelation(request.supplierId(), request.shopId());

        String source = "SUPPLIER".equals(actorRole) ? "SUPPLIER" : "SHOP";

        Order order = Order.create(
                request.supplierId(),
                request.shopId(),
                actorUserId,
                actorRole,
                source,
                Boolean.TRUE.equals(request.asapPayment()),
                request.currency(),
                request.paymentTerms()
        );
        if (request.notes() != null) {
            order.setNotes(request.notes());
        }
        if (request.globalDiscount() != null) {
            order.setGlobalDiscount(request.globalDiscount());
        }
        if ("SUPPLIER".equals(actorRole) && request.taxRate() != null) {
            order.setTaxRate(request.taxRate());
        }

        Order savedOrder = orders.save(order);

        List<OrderItem> items = new ArrayList<>();
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
            item.setOrderId(savedOrder.getId());
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
            items.add(savedItem);
            subtotal = subtotal.add(savedItem.getLineTotal());
        }

        savedOrder.setSubtotal(subtotal);
        savedOrder.recalculateTotals();
        orders.save(savedOrder);

        events.save(OrderEvent.create(savedOrder.getId(), "ORDER_CREATED", actorUserId, null));
        outbox.append(new OrderEvents.OrderCreatedEvent(UUID.randomUUID(), Instant.now(),
                savedOrder.getId(), savedOrder.getReference(),
                savedOrder.getShopId(), savedOrder.getSupplierId(),
                actorUserId, source),
                String.valueOf(savedOrder.getId()));

        if ("SUPPLIER".equals(source)) {
            savedOrder.confirm();
            savedOrder.prepare();
            orders.save(savedOrder);
            balanceUseCase.creditBalance(savedOrder.getSupplierId(), savedOrder.getShopId(),
                    savedOrder.getTotal(), savedOrder.getId(), actorUserId);
            events.save(OrderEvent.create(savedOrder.getId(), "ORDER_CONFIRMED", actorUserId, "Auto-confirmé (source SUPPLIER)"));
            events.save(OrderEvent.create(savedOrder.getId(), "ORDER_PREPARING", actorUserId, "Auto-mis en préparation (source SUPPLIER)"));
            outbox.append(new OrderEvents.OrderConfirmedEvent(UUID.randomUUID(), Instant.now(),
                    savedOrder.getId(), savedOrder.getReference(),
                    savedOrder.getShopId(), savedOrder.getSupplierId(), actorUserId),
                    String.valueOf(savedOrder.getId()));
            outbox.append(new OrderEvents.OrderPreparingEvent(UUID.randomUUID(), Instant.now(),
                    savedOrder.getId(), savedOrder.getReference(),
                    savedOrder.getShopId(), savedOrder.getSupplierId(), actorUserId),
                    String.valueOf(savedOrder.getId()));
        }

        return OrderResponse.from(savedOrder, items);
    }

    private void validateOrganizations(UUID supplierId, UUID shopId) {
        organizations.findById(new com.paymentplatform.organization.domain.valueobject.OrganizationId(supplierId))
                .orElseThrow(() -> new NotFoundException("Fournisseur non trouvé : " + supplierId));
        organizations.findById(new com.paymentplatform.organization.domain.valueobject.OrganizationId(shopId))
                .orElseThrow(() -> new NotFoundException("Boutique non trouvée : " + shopId));
    }

    private void validateRelation(UUID supplierId, UUID shopId) {
        if (!relations.existsBySupplierIdAndShopIdAndStatus(
                new com.paymentplatform.organization.domain.valueobject.OrganizationId(supplierId),
                new com.paymentplatform.organization.domain.valueobject.OrganizationId(shopId),
                com.paymentplatform.organization.domain.valueobject.RelationStatus.ACTIVE)) {
            throw new ConflictException("Aucune relation active entre le fournisseur et la boutique");
        }
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
