package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.dto.OrderQuoteResponse;
import com.paymentplatform.organization.application.dto.SendQuoteRequest;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderFiscalizer;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.OrderQuote;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.OrderEventRepository;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderQuoteRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Devis fournisseur : sur la demande boutique (BROUILLON ou CONFIRMÉE), le
 * fournisseur modifie les lignes (quantités), pose les remises (lignes en %
 * + globale en %), recalcule la fiscalité et envoie le devis versionné.
 * La boutique est notifiée (elle consulte et télécharge chaque devis).
 */
@Service
public class SendQuoteUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderQuoteRepository quotes;
    private final OrderEventRepository events;
    private final ProductRepository products;
    private final OrganizationRepository organizations;
    private final OutboxEventStore outbox;

    public SendQuoteUseCase(OrderRepository orders, OrderItemRepository orderItems,
                            OrderQuoteRepository quotes, OrderEventRepository events,
                            ProductRepository products, OrganizationRepository organizations,
                            OutboxEventStore outbox) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.quotes = quotes;
        this.events = events;
        this.products = products;
        this.organizations = organizations;
        this.outbox = outbox;
    }

    @Transactional
    public OrderQuoteResponse execute(UUID orderId, SendQuoteRequest request,
                                      UUID supplierId, UUID actorUserId) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new ConflictException("Le devis doit contenir au moins un article");
        }
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        if (supplierId == null || !supplierId.equals(order.getSupplierId())) {
            throw new ForbiddenException("Seul le fournisseur de la commande peut envoyer un devis");
        }
        if (!"DRAFT".equals(order.getStatus()) && !"CONFIRMED".equals(order.getStatus())) {
            throw new ConflictException("Le devis n'est possible que sur une demande en brouillon ou confirmée");
        }
        BigDecimal globalDiscount = request.globalDiscount() != null ? request.globalDiscount() : BigDecimal.ZERO;
        if (globalDiscount.signum() < 0 || globalDiscount.compareTo(new BigDecimal("100")) > 0) {
            throw new ConflictException("La remise globale doit être entre 0 et 100 %");
        }

        List<OrderItem> oldItems = orderItems.findByOrderId(orderId);
        for (OrderItem oldItem : oldItems) {
            products.findByIdForUpdate(oldItem.getProductId()).ifPresent(product -> {
                product.setReservedQty(Math.max(0, product.getReservedQty() - oldItem.getQuantity()));
                products.save(product);
            });
        }
        orderItems.deleteAll(oldItems);

        Organization shop = organizations.findById(
                        new com.paymentplatform.organization.domain.valueobject.OrganizationId(order.getShopId()))
                .orElseThrow(() -> new NotFoundException("Boutique non trouvée : " + order.getShopId()));

        List<OrderItem> newItems = new ArrayList<>();
        List<BigDecimal> discountPcts = new ArrayList<>();
        for (OrderItemRequest itemReq : request.items()) {
            Product product = products.findById(itemReq.productId())
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé : " + itemReq.productId()));
            int availableQty = product.getQuantity() - product.getReservedQty();
            if (availableQty < itemReq.quantity()) {
                throw new ConflictException("Stock insuffisant pour le produit " + product.getName()
                        + " (disponible: " + availableQty + ", demandé: " + itemReq.quantity() + ")");
            }
            BigDecimal pct = itemReq.discount() != null ? itemReq.discount() : BigDecimal.ZERO;
            if (pct.signum() < 0 || pct.compareTo(new BigDecimal("100")) > 0) {
                throw new ConflictException("La remise ligne doit être entre 0 et 100 %");
            }
            // Le fournisseur saisit un % ; le moteur attend un montant : conversion.
            BigDecimal discountAmount = product.getUnitPrice() != null
                    ? product.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()))
                            .multiply(pct).divide(new BigDecimal("100"), 4, java.math.RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            OrderItem item = new OrderItem();
            item.setOrderId(orderId);
            item.setProductId(product.getId());
            item.setProductRef(product.getSku());
            item.setProductName(product.getName());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(product.getUnitPrice());
            item.setDiscount(discountAmount);
            CreateOrderUseCase.snapshotFiscalFields(item, product);
            product.setReservedQty(product.getReservedQty() + itemReq.quantity());
            products.save(product);
            newItems.add(item);
            discountPcts.add(pct);
        }

        order.setGlobalDiscount(globalDiscount);
        OrderFiscalizer.apply(order, newItems, shop);
        orders.save(order);
        List<OrderItem> savedItems = new ArrayList<>();
        for (OrderItem item : newItems) {
            savedItems.add(orderItems.save(item));
        }

        int version = quotes.findByOrderId(orderId).stream()
                .mapToInt(OrderQuote::version).max().orElse(0) + 1;
        OrderQuote quote = OrderQuote.builder()
                .id(UUID.randomUUID())
                .orderId(orderId)
                .orderReference(order.getReference())
                .supplierId(order.getSupplierId())
                .version(version)
                .globalDiscount(order.getGlobalDiscount())
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .total(order.getTotal())
                .stampAmount(order.getStampAmount())
                .currency(order.getCurrency())
                .linesJson(buildLinesJson(savedItems, discountPcts))
                .createdBy(actorUserId)
                .createdAt(Instant.now())
                .build();
        quotes.save(quote);

        events.save(OrderEvent.create(orderId, "QUOTE_SENT", actorUserId,
                "Devis n°" + version + " envoyé"));
        outbox.append(new OrderEvents.QuoteSentEvent(UUID.randomUUID(), Instant.now(),
                orderId, order.getReference(), order.getShopId(), order.getSupplierId(),
                version, order.getTotal().toPlainString()), String.valueOf(orderId));

        return OrderQuoteResponse.from(quote);
    }

    private String buildLinesJson(List<OrderItem> items, List<BigDecimal> discountPcts) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < items.size(); i++) {
            OrderItem it = items.get(i);
            if (i > 0) {
                sb.append(",");
            }
            BigDecimal pct = i < discountPcts.size() && discountPcts.get(i) != null
                    ? discountPcts.get(i) : BigDecimal.ZERO;
            sb.append("{\"ref\":\"").append(esc(it.getProductRef()))
                    .append("\",\"name\":\"").append(esc(it.getProductName()))
                    .append("\",\"qty\":").append(it.getQuantity())
                    .append(",\"unitPrice\":").append(n(it.getUnitPrice()))
                    .append(",\"discountPct\":").append(pct.stripTrailingZeros().toPlainString())
                    .append(",\"discount\":").append(n(it.getDiscount()))
                    .append(",\"lineTotal\":").append(n(it.getLineTotal()))
                    .append(",\"vatRate\":").append(n(it.getVatRate()))
                    .append("}");
        }
        return sb.append("]").toString();
    }

    private String n(BigDecimal v) {
        return v != null ? v.toPlainString() : "0";
    }

    private String esc(String v) {
        if (v == null) {
            return "";
        }
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
