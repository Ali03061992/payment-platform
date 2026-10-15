package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateQuoteRequestRequest;
import com.paymentplatform.organization.application.dto.QuoteRequestResponse;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.model.QuoteRequest;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.organization.domain.repository.QuoteRequestRepository;
import com.paymentplatform.organization.domain.repository.SupplierShopRelationRepository;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Demande de devis boutique (entité dédiée, pas une commande).
 * Aucune remise côté boutique : seules quantités + notes.
 */
@Service
public class CreateQuoteRequestUseCase {

    private final QuoteRequestRepository requests;
    private final ProductRepository products;
    private final OrganizationRepository organizations;
    private final SupplierShopRelationRepository relations;
    private final OutboxEventStore outbox;

    public CreateQuoteRequestUseCase(QuoteRequestRepository requests, ProductRepository products,
                                     OrganizationRepository organizations,
                                     SupplierShopRelationRepository relations,
                                     OutboxEventStore outbox) {
        this.requests = requests;
        this.products = products;
        this.organizations = organizations;
        this.relations = relations;
        this.outbox = outbox;
    }

    @Transactional
    public QuoteRequestResponse execute(CreateQuoteRequestRequest request, UUID shopId, UUID actorUserId) {
        if (request.items() == null || request.items().isEmpty()) {
            throw new ConflictException("La demande doit contenir au moins un article");
        }
        if (request.supplierId().equals(shopId)) {
            throw new ConflictException("Le fournisseur et la boutique doivent être différents");
        }
        Organization supplier = organizations.findById(
                        new com.paymentplatform.organization.domain.valueobject.OrganizationId(request.supplierId()))
                .orElseThrow(() -> new NotFoundException("Fournisseur non trouvé : " + request.supplierId()));
        Organization shop = organizations.findById(
                        new com.paymentplatform.organization.domain.valueobject.OrganizationId(shopId))
                .orElseThrow(() -> new NotFoundException("Boutique non trouvée : " + shopId));
        if (!relations.existsBySupplierIdAndShopIdAndStatus(
                new com.paymentplatform.organization.domain.valueobject.OrganizationId(request.supplierId()),
                new com.paymentplatform.organization.domain.valueobject.OrganizationId(shopId),
                com.paymentplatform.organization.domain.valueobject.RelationStatus.ACTIVE)) {
            throw new ConflictException("Aucune relation active entre le fournisseur et la boutique");
        }

        StringBuilder lines = new StringBuilder("[");
        boolean first = true;
        for (CreateQuoteRequestRequest.QuoteRequestLine line : request.items()) {
            if (line.quantity() == null || line.quantity() < 1) {
                throw new ConflictException("Quantité invalide");
            }
            Product product = products.findById(line.productId())
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé : " + line.productId()));
            if (!product.getSupplierId().equals(request.supplierId())) {
                throw new ForbiddenException("Produit hors catalogue du fournisseur");
            }
            if (!first) {
                lines.append(",");
            }
            first = false;
            lines.append("{\"productId\":\"").append(product.getId())
                    .append("\",\"ref\":\"").append(esc(product.getSku()))
                    .append("\",\"name\":\"").append(esc(product.getName()))
                    .append("\",\"qty\":").append(line.quantity())
                    .append(",\"unitPrice\":").append(product.getUnitPrice() != null
                            ? product.getUnitPrice().toPlainString() : "0")
                    .append("}");
        }
        lines.append("]");

        String reference = "DEV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                + "-" + System.currentTimeMillis();
        QuoteRequest quoteRequest = QuoteRequest.builder()
                .id(UUID.randomUUID())
                .reference(reference)
                .supplierId(request.supplierId())
                .supplierName(supplier.name().value())
                .shopId(shopId)
                .shopName(shop.name().value())
                .notes(request.notes())
                .linesJson(lines.toString())
                .createdBy(actorUserId)
                .createdAt(Instant.now())
                .build();
        requests.save(quoteRequest);

        outbox.append(new OrderEvents.QuoteRequestedEvent(UUID.randomUUID(), Instant.now(),
                quoteRequest.id(), reference, shopId, request.supplierId(), shop.name().value()),
                String.valueOf(quoteRequest.id()));

        return QuoteRequestResponse.from(quoteRequest, List.of());
    }

    private String esc(String v) {
        if (v == null) {
            return "";
        }
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
