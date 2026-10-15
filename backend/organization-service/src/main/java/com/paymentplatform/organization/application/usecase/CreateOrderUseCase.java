package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderFiscalizer;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.*;
import com.paymentplatform.organization.domain.valueobject.VatRate;
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
        // request.taxRate() global IGNORÉ : la TVA est portée par chaque produit
        // (art. 18 Code TVA — ventilation par taux). Champ conservé pour compatibilité.

        Organization shop = organizations.findById(
                        new com.paymentplatform.organization.domain.valueobject.OrganizationId(request.shopId()))
                .orElseThrow(() -> new NotFoundException("Boutique non trouvée : " + request.shopId()));

        Order savedOrder = orders.save(order);

        List<OrderItem> items = new ArrayList<>();

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
            snapshotFiscalFields(item, product);
            item.setProductSnapshot(buildProductSnapshot(product));

            product.setReservedQty(product.getReservedQty() + itemReq.quantity());
            products.save(product);

            items.add(item);
        }

        // Régime acheteur (5 cas sans TVA) + remise→FODEC→TVA par taux + timbre.
        OrderFiscalizer.apply(savedOrder, items, shop);
        orders.save(savedOrder);
        List<OrderItem> savedItems = new ArrayList<>();
        for (OrderItem item : items) {
            savedItems.add(orderItems.save(item));
        }
        items = savedItems;

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

    /**
     * Snapshot fiscal de la fiche produit vers la ligne (taux + FODEC).
     * Un produit à 0% exige son motif d'exonération par nature.
     */
    static void snapshotFiscalFields(OrderItem item, Product product) {
        VatRate rate = VatRate.of(product.getVatRate());
        if (rate.isExempt() && (product.getVatExemptionText() == null
                || product.getVatExemptionText().isBlank())) {
            throw new ConflictException("Produit « " + product.getName()
                    + " » à TVA 0% sans motif d'exonération : renseignez le motif sur la fiche produit");
        }
        item.setVatRate(rate.value());
        item.setFodecApplicable(product.isFodecApplicable());
        item.setPriceTtc(product.isPriceTtc());
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
