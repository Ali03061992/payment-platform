package com.paymentplatform.organization.application.usecase;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderFiscalizer;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.model.PaymentTerms;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.model.QuoteRequest;
import com.paymentplatform.organization.domain.model.QuoteRequestStatus;
import com.paymentplatform.organization.domain.model.SupplierQuote;
import com.paymentplatform.organization.domain.repository.OrderEventRepository;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.organization.domain.repository.QuoteRequestRepository;
import com.paymentplatform.organization.domain.repository.SupplierQuoteRepository;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Acceptation d'un devis par la boutique (« J'accepte ce devis ») : crée la
 * commande CONFIRMÉE aux conditions figées du devis (lignes, remises %,
 * totaux), réserve le stock et notifie le fournisseur.
 */
@Service
public class AcceptQuoteUseCase {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final QuoteRequestRepository requests;
    private final SupplierQuoteRepository quotes;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final ProductRepository products;
    private final OrganizationRepository organizations;
    private final BalanceUseCase balanceUseCase;
    private final OutboxEventStore outbox;

    public AcceptQuoteUseCase(QuoteRequestRepository requests, SupplierQuoteRepository quotes,
                              OrderRepository orders, OrderItemRepository orderItems,
                              OrderEventRepository events, ProductRepository products,
                              OrganizationRepository organizations, BalanceUseCase balanceUseCase,
                              OutboxEventStore outbox) {
        this.requests = requests;
        this.quotes = quotes;
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.products = products;
        this.organizations = organizations;
        this.balanceUseCase = balanceUseCase;
        this.outbox = outbox;
    }

    @Transactional
    public OrderResponse execute(UUID quoteId, UUID shopId, UUID actorUserId) {
        SupplierQuote quote = quotes.findById(quoteId)
                .orElseThrow(() -> new NotFoundException("Devis non trouvé : " + quoteId));
        QuoteRequest request = requests.findById(quote.requestId())
                .orElseThrow(() -> new NotFoundException("Demande non trouvée : " + quote.requestId()));
        if (shopId == null || !shopId.equals(request.shopId())) {
            throw new ForbiddenException("Seule la boutique demanderesse peut accepter ce devis");
        }
        if (request.status() != QuoteRequestStatus.QUOTED) {
            throw new ConflictException("Demande non acceptable en l'état : " + request.status());
        }

        Organization shop = organizations.findById(
                        new com.paymentplatform.organization.domain.valueobject.OrganizationId(request.shopId()))
                .orElseThrow(() -> new NotFoundException("Boutique non trouvée : " + request.shopId()));

        Order order = Order.create(request.supplierId(), request.shopId(), actorUserId,
                "SHOP_ADMIN", "QUOTE", false, quote.currency(), PaymentTerms.IMMEDIATE);
        order.setNotes("Devis " + request.reference() + " n°" + quote.version() + " accepté");
        Order savedOrder = orders.save(order);

        List<OrderItem> items = new ArrayList<>();
        for (QuoteLine line : parseLines(quote.linesJson())) {
            Product product = products.findById(line.productId())
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé : " + line.productId()));
            int availableQty = product.getQuantity() - product.getReservedQty();
            if (availableQty < line.qty()) {
                throw new ConflictException("Stock insuffisant pour le produit " + product.getName()
                        + " (disponible: " + availableQty + ", demandé: " + line.qty() + ")");
            }
            BigDecimal discountAmount = product.getUnitPrice() != null
                    ? product.getUnitPrice().multiply(BigDecimal.valueOf(line.qty()))
                            .multiply(line.discountPct()).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            OrderItem item = new OrderItem();
            item.setOrderId(savedOrder.getId());
            item.setProductId(product.getId());
            item.setProductRef(product.getSku());
            item.setProductName(product.getName());
            item.setQuantity(line.qty());
            item.setUnitPrice(product.getUnitPrice());
            item.setDiscount(discountAmount);
            CreateOrderUseCase.snapshotFiscalFields(item, product);
            product.setReservedQty(product.getReservedQty() + line.qty());
            products.save(product);
            items.add(item);
        }

        OrderFiscalizer.apply(savedOrder, items, shop);
        savedOrder.confirm();
        orders.save(savedOrder);
        List<OrderItem> savedItems = new ArrayList<>();
        for (OrderItem item : items) {
            savedItems.add(orderItems.save(item));
        }
        balanceUseCase.creditBalance(savedOrder.getSupplierId(), savedOrder.getShopId(),
                savedOrder.getTotal(), savedOrder.getId(), actorUserId);

        events.save(OrderEvent.create(savedOrder.getId(), "ORDER_CREATED", actorUserId,
                "Créée par acceptation du devis " + request.reference() + " n°" + quote.version()));
        outbox.append(new OrderEvents.OrderCreatedEvent(UUID.randomUUID(), Instant.now(),
                savedOrder.getId(), savedOrder.getReference(),
                savedOrder.getShopId(), savedOrder.getSupplierId(), actorUserId, "QUOTE"),
                String.valueOf(savedOrder.getId()));
        events.save(OrderEvent.create(savedOrder.getId(), "ORDER_CONFIRMED", actorUserId,
                "Confirmée par acceptation du devis"));
        outbox.append(new OrderEvents.OrderConfirmedEvent(UUID.randomUUID(), Instant.now(),
                savedOrder.getId(), savedOrder.getReference(),
                savedOrder.getShopId(), savedOrder.getSupplierId(), actorUserId),
                String.valueOf(savedOrder.getId()));

        request.markAccepted();
        requests.save(request);
        outbox.append(new OrderEvents.QuoteAcceptedEvent(UUID.randomUUID(), Instant.now(),
                request.id(), request.reference(), request.shopId(), request.supplierId(),
                savedOrder.getId(), savedOrder.getReference(), savedOrder.getTotal().toPlainString()),
                String.valueOf(request.id()));

        return OrderResponse.from(savedOrder, savedItems);
    }

    private record QuoteLine(UUID productId, int qty, BigDecimal discountPct) {}

    private List<QuoteLine> parseLines(String json) {
        List<QuoteLine> lines = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(json != null ? json : "[]");
            if (root.isArray()) {
                for (JsonNode n : root) {
                    UUID productId = UUID.fromString(n.get("productId").asText());
                    int qty = n.has("qty") ? n.get("qty").asInt(1) : 1;
                    BigDecimal pct = BigDecimal.ZERO;
                    try {
                        if (n.has("discountPct") && !n.get("discountPct").isNull()) {
                            pct = new BigDecimal(n.get("discountPct").asText("0"));
                        }
                    } catch (Exception ignored) {
                        pct = BigDecimal.ZERO;
                    }
                    lines.add(new QuoteLine(productId, Math.max(1, qty), pct));
                }
            }
        } catch (Exception e) {
            throw new ConflictException("Devis illisible, acceptation impossible");
        }
        if (lines.isEmpty()) {
            throw new ConflictException("Devis vide, acceptation impossible");
        }
        return lines;
    }
}
