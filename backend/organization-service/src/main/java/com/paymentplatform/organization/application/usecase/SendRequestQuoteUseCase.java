package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.dto.SendQuoteRequest;
import com.paymentplatform.organization.application.dto.SupplierQuoteResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderFiscalizer;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.model.QuoteRequest;
import com.paymentplatform.organization.domain.model.QuoteRequestStatus;
import com.paymentplatform.organization.domain.model.SupplierQuote;
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
 * Chiffrage fournisseur d'une demande (via l'application) : le fournisseur
 * ajuste les quantités, pose les remises (lignes en % + globale en %),
 * la fiscalité est recalculée et le devis versionné est envoyé à la boutique.
 * Disponibilité vérifiée SANS réservation (réservé à l'acceptation).
 */
@Service
public class SendRequestQuoteUseCase {

    private final QuoteRequestRepository requests;
    private final SupplierQuoteRepository quotes;
    private final ProductRepository products;
    private final OrganizationRepository organizations;
    private final OutboxEventStore outbox;

    public SendRequestQuoteUseCase(QuoteRequestRepository requests, SupplierQuoteRepository quotes,
                                   ProductRepository products, OrganizationRepository organizations,
                                   OutboxEventStore outbox) {
        this.requests = requests;
        this.quotes = quotes;
        this.products = products;
        this.organizations = organizations;
        this.outbox = outbox;
    }

    @Transactional
    public SupplierQuoteResponse execute(UUID requestId, SendQuoteRequest request,
                                         UUID supplierId, UUID actorUserId) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new ConflictException("Le devis doit contenir au moins un article");
        }
        QuoteRequest quoteRequest = requests.findById(requestId)
                .orElseThrow(() -> new NotFoundException("Demande non trouvée : " + requestId));
        if (supplierId == null || !supplierId.equals(quoteRequest.supplierId())) {
            throw new ForbiddenException("Seul le fournisseur destinataire peut chiffrer");
        }
        if (quoteRequest.status() != QuoteRequestStatus.PENDING
                && quoteRequest.status() != QuoteRequestStatus.QUOTED) {
            throw new ConflictException("Demande déjà clôturée : " + quoteRequest.status());
        }
        BigDecimal globalDiscount = request.globalDiscount() != null ? request.globalDiscount() : BigDecimal.ZERO;
        if (globalDiscount.signum() < 0 || globalDiscount.compareTo(new BigDecimal("100")) > 0) {
            throw new ConflictException("La remise globale doit être entre 0 et 100 %");
        }

        Organization shop = organizations.findById(
                        new com.paymentplatform.organization.domain.valueobject.OrganizationId(quoteRequest.shopId()))
                .orElseThrow(() -> new NotFoundException("Boutique non trouvée : " + quoteRequest.shopId()));

        List<OrderItem> priced = new ArrayList<>();
        List<BigDecimal> discountPcts = new ArrayList<>();
        for (OrderItemRequest itemReq : request.items()) {
            Product product = products.findById(itemReq.productId())
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé : " + itemReq.productId()));
            if (!product.getSupplierId().equals(quoteRequest.supplierId())) {
                throw new ForbiddenException("Produit hors catalogue du fournisseur");
            }
            int availableQty = product.getQuantity() - product.getReservedQty();
            if (availableQty < itemReq.quantity()) {
                throw new ConflictException("Stock insuffisant pour le produit " + product.getName()
                        + " (disponible: " + availableQty + ", demandé: " + itemReq.quantity() + ")");
            }
            BigDecimal pct = itemReq.discount() != null ? itemReq.discount() : BigDecimal.ZERO;
            if (pct.signum() < 0 || pct.compareTo(new BigDecimal("100")) > 0) {
                throw new ConflictException("La remise ligne doit être entre 0 et 100 %");
            }
            BigDecimal discountAmount = product.getUnitPrice() != null
                    ? product.getUnitPrice().multiply(BigDecimal.valueOf(itemReq.quantity()))
                            .multiply(pct).divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO;
            OrderItem item = new OrderItem();
            item.setOrderId(UUID.randomUUID());
            item.setProductId(product.getId());
            item.setProductRef(product.getSku());
            item.setProductName(product.getName());
            item.setQuantity(itemReq.quantity());
            item.setUnitPrice(product.getUnitPrice());
            item.setDiscount(discountAmount);
            item.setFodecApplicable(product.isFodecApplicable());
            item.setPriceTtc(product.isPriceTtc());
            CreateOrderUseCase.snapshotFiscalFields(item, product);
            priced.add(item);
            discountPcts.add(pct);
        }

        Order transientOrder = Order.create(quoteRequest.supplierId(), quoteRequest.shopId(),
                actorUserId, "SUPPLIER_ADMIN", "QUOTE", false, "TND",
                com.paymentplatform.organization.domain.model.PaymentTerms.IMMEDIATE);
        transientOrder.setGlobalDiscount(globalDiscount);
        OrderFiscalizer.apply(transientOrder, priced, shop);

        int version = quotes.findByRequestId(requestId).stream()
                .mapToInt(SupplierQuote::version).max().orElse(0) + 1;
        SupplierQuote quote = SupplierQuote.builder()
                .id(UUID.randomUUID())
                .requestId(requestId)
                .requestReference(quoteRequest.reference())
                .supplierId(quoteRequest.supplierId())
                .version(version)
                .globalDiscount(transientOrder.getGlobalDiscount())
                .subtotal(transientOrder.getSubtotal())
                .taxAmount(transientOrder.getTaxAmount())
                .total(transientOrder.getTotal())
                .stampAmount(transientOrder.getStampAmount())
                .currency(transientOrder.getCurrency())
                .linesJson(buildLinesJson(priced, discountPcts))
                .createdBy(actorUserId)
                .createdAt(Instant.now())
                .build();
        quotes.save(quote);
        quoteRequest.markQuoted();
        requests.save(quoteRequest);

        outbox.append(new OrderEvents.QuoteSentEvent(UUID.randomUUID(), Instant.now(),
                requestId, quoteRequest.reference(), quoteRequest.shopId(), quoteRequest.supplierId(),
                version, transientOrder.getTotal().toPlainString()), String.valueOf(requestId));

        return SupplierQuoteResponse.from(quote);
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
            // Total ligne HT figé (transitoire : non calculé par @PrePersist).
            BigDecimal gross = it.getUnitPrice() != null
                    ? it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity())) : BigDecimal.ZERO;
            BigDecimal frozenTotal = gross.subtract(it.getDiscount() != null ? it.getDiscount() : BigDecimal.ZERO);
            sb.append("{\"productId\":\"").append(it.getProductId())
                    .append("\",\"ref\":\"").append(esc(it.getProductRef()))
                    .append("\",\"name\":\"").append(esc(it.getProductName()))
                    .append("\",\"qty\":").append(it.getQuantity())
                    .append(",\"unitPrice\":").append(n(it.getUnitPrice()))
                    .append(",\"discountPct\":").append(pct.stripTrailingZeros().toPlainString())
                    .append(",\"discount\":").append(n(it.getDiscount()))
                    .append(",\"lineTotal\":").append(frozenTotal.toPlainString())
                    .append(",\"vatRate\":").append(n(it.getVatRate()))
                    .append(",\"fodec\":").append(it.isFodecApplicable())
                    .append(",\"priceTtc\":").append(it.isPriceTtc())
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
