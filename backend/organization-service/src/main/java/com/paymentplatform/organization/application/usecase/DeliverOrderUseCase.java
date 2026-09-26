package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.repository.OrderEventRepository;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.infrastructure.http.PaymentClient;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.domain.exception.NotFoundException;
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
 * l'événement et déclenche le paiement automatique ASAP quand requis.
 */
@Service
public class DeliverOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(DeliverOrderUseCase.class);

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final OutboxEventStore outbox;
    private final PaymentClient paymentClient;

    public DeliverOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                               OrderEventRepository events, OutboxEventStore outbox,
                               PaymentClient paymentClient) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.outbox = outbox;
        this.paymentClient = paymentClient;
    }

    /**
     * Déclare une commande livrée et tente la création du paiement ASAP associé.
     *
     * @param orderId identifiant de la commande
     * @param receivedBy destinataire ayant réceptionné (identifiant utilisateur)
     * @param actorUserId auteur de la déclaration
     * @return commande livrée sous forme de réponse
     */
    @Transactional
    public OrderResponse execute(UUID orderId, UUID receivedBy, UUID actorUserId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        order.deliver(receivedBy);
        orders.save(order);

        events.save(OrderEvent.create(orderId, "ORDER_DELIVERED", actorUserId,
                "Reçu par: " + receivedBy));
        outbox.append(new OrderEvents.OrderDeliveredEvent(UUID.randomUUID(), Instant.now(),
                orderId, order.getReference(),
                order.getShopId(), order.getSupplierId(),
                actorUserId, receivedBy),
                String.valueOf(orderId));

        if (order.isAsapPayment()) {
            try {
                paymentClient.createAutoPayment(order.getShopId(), order.getSupplierId(),
                        order.getCurrency(), actorUserId, order.getTotal(), orderId);
                log.info("Auto-payment ASAP créé pour la commande {} lors de la livraison", order.getReference());
            } catch (Exception e) {
                log.error("Erreur lors de la création du paiement ASAP pour la commande {}", order.getReference(), e);
            }
        }

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        return OrderResponse.from(order, items);
    }
}
