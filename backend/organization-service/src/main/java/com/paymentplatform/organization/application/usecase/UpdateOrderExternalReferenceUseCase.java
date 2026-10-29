package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.application.security.TenantAccess;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.security.ActorContext;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Enregistre la référence électronique externe d'une facture (plateforme
 * TTN/El Fatoora pour les e-factures, ou TEJ) : affichée avec QR en haut à
 * droite, comme les outils tunisiens (référence unique + QR de validation).
 */
@Service
public class UpdateOrderExternalReferenceUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final AuditRecorder audit;

    public UpdateOrderExternalReferenceUseCase(OrderRepository orders,
                                               OrderItemRepository orderItems,
                                               AuditRecorder audit) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.audit = audit;
    }

    @Transactional
    public OrderResponse execute(UUID orderId, String reference, ActorContext actor) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        TenantAccess.requireSupplier(actor, order.getSupplierId());
        if (reference == null || reference.isBlank()) {
            throw new ConflictException("La référence électronique est obligatoire");
        }
        String previous = order.getExternalReference();
        order.setExternalReference(reference.trim());
        Order saved = orders.save(order);
        audit.record(actor.userId(), actor.organizationId(),
                AuditActions.ORDER_EXTERNAL_REFERENCE_UPDATED, orderId,
                "{\"orderReference\":\"" + saved.getReference()
                        + "\",\"previous\":\"" + (previous == null ? "" : previous)
                        + "\",\"new\":\"" + saved.getExternalReference() + "\"}");
        return OrderResponse.from(saved, orderItems.findByOrderId(orderId));
    }
}
