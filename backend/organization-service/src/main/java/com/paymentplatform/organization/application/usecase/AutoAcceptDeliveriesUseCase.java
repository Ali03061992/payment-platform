package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.repository.OrderEventRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Acceptation automatique des livraisons restées sans réponse : une commande
 * livrée (DELIVERED) ni acceptée ni rejetée sous {@code app.orders.delivery-auto-accept-minutes}
 * minutes (défaut 15) est acceptée d'office, comme si la boutique avait confirmé
 * la réception. Le paiement auto a déjà été créé à la livraison.
 */
@Service
public class AutoAcceptDeliveriesUseCase {

    private static final Logger log = LoggerFactory.getLogger(AutoAcceptDeliveriesUseCase.class);

    private final OrderRepository orders;
    private final OrderEventRepository events;
    private final AcceptOrderUseCase acceptOrder;
    private final long autoAcceptMinutes;

    public AutoAcceptDeliveriesUseCase(OrderRepository orders,
                                       OrderEventRepository events,
                                       AcceptOrderUseCase acceptOrder,
                                       @Value("${app.orders.delivery-auto-accept-minutes:15}") long autoAcceptMinutes) {
        this.orders = orders;
        this.events = events;
        this.acceptOrder = acceptOrder;
        this.autoAcceptMinutes = autoAcceptMinutes;
    }

    @Transactional
    public int execute() {
        Instant cutoff = Instant.now().minus(autoAcceptMinutes, ChronoUnit.MINUTES);
        List<Order> pending = orders.findByStatusAndDeliveredAtBefore("DELIVERED", cutoff);
        log.info("Found {} delivered orders awaiting reception for more than {} minutes",
                pending.size(), autoAcceptMinutes);

        int accepted = 0;
        for (Order order : pending) {
            try {
                // AcceptOrderUseCase décrémente le stock et publie OrderAcceptedEvent ;
                // on trace en plus l'origine automatique (audit local uniquement,
                // un seul événement outbox pour ne pas notifier deux fois).
                acceptOrder.execute(order.getId(), null);
                events.save(OrderEvent.create(order.getId(), "ORDER_AUTO_ACCEPTED", null,
                        "Réception acceptée automatiquement après " + autoAcceptMinutes + " min sans réponse"));
                accepted++;
                log.info("Auto-accepted delivery for order {}", order.getReference());
            } catch (Exception e) {
                log.error("Failed to auto-accept delivery for order {}: {}",
                        order.getReference(), e.getMessage(), e);
            }
        }
        return accepted;
    }
}
