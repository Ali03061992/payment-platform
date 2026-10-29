package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.application.security.TenantAccess;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.repository.OrderEventRepository;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.security.ActorContext;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Cas d'usage de livraison d'une commande : bascule le statut, journalise
 * l'événement et crée le paiement automatiquement dès réception confirmée
 * (agent ou boutique). Idempotent par commande : un seul paiement, jamais
 * de doublon, jamais sur commande annulée/rejetée.
 */
@Service
public class DeliverOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeliverOrderUseCase.class);

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final OutboxEventStore outbox;
    private final IssueWithholdingCertificateUseCase withholding;

    public DeliverOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                               OrderEventRepository events, OutboxEventStore outbox,
                               IssueWithholdingCertificateUseCase withholding) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.outbox = outbox;
        this.withholding = withholding;
    }

    /**
     * Déclare une commande livrée (réception confirmée) et crée son paiement.
     *
     * @param orderId identifiant de la commande
     * @param receivedBy destinataire ayant réceptionné (identifiant utilisateur)
     * @param actorUserId auteur de la déclaration
     * @return commande livrée sous forme de réponse
     */
    @Transactional
    public OrderResponse execute(UUID orderId, UUID receivedBy, ActorContext actor) {
        return execute(orderId, receivedBy, actor, null);
    }

    @Transactional
    public OrderResponse execute(UUID orderId, UUID receivedBy, ActorContext actor, String withholdingNature) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        TenantAccess.requireDeliveryActor(actor, order.getSupplierId(), order.getDeliveryAgentId());
        return deliver(order, receivedBy, actor.userId(), withholdingNature);
    }

    @Deprecated
    @Transactional
    public OrderResponse execute(UUID orderId, UUID receivedBy, UUID actorUserId) {
        return execute(orderId, receivedBy, actorUserId, null);
    }

    /**
     * Déclare une commande livrée (réception confirmée), crée son paiement et émet
     * son certificat de retenue (art. 55 IRPP/IS, TEJ) — sans assiette, aucun
     * certificat. L'émission n'échoue jamais la livraison (tracée en log).
     *
     * @param withholdingNature nature du montant (MARCHANDISES par défaut)
     */
    @Deprecated
    @Transactional
    public OrderResponse execute(UUID orderId, UUID receivedBy, UUID actorUserId, String withholdingNature) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        return deliver(order, receivedBy, actorUserId, withholdingNature);
    }

    private OrderResponse deliver(Order order, UUID receivedBy, UUID actorUserId, String withholdingNature) {
        UUID orderId = order.getId();
        order.deliver(receivedBy);
        orders.save(order);

        try {
            withholding.issueForDeliveredOrder(order, withholdingNature);
        } catch (Exception e) {
            log.error("Émission du certificat de retenue impossible pour la commande {}",
                    order.getReference(), e);
        }

        events.save(OrderEvent.create(orderId, "ORDER_DELIVERED", actorUserId,
                "Reçu par: " + (receivedBy != null ? receivedBy : actorUserId)));
        outbox.append(new OrderEvents.OrderDeliveredEvent(UUID.randomUUID(), Instant.now(),
                orderId, order.getReference(),
                order.getShopId(), order.getSupplierId(),
                actorUserId, receivedBy),
                String.valueOf(orderId));

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        return OrderResponse.from(order, items);
    }
}
