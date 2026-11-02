package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.DisputeResponse;
import com.paymentplatform.organization.application.security.TenantAccess;
import com.paymentplatform.organization.domain.model.Dispute;
import com.paymentplatform.organization.domain.model.DisputePriority;
import com.paymentplatform.organization.domain.model.DisputeStatusHistory;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.repository.DisputeRepository;
import com.paymentplatform.organization.domain.repository.DisputeStatusHistoryRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.shared.domain.event.DisputeEvents;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.security.ActorContext;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CreateDisputeUseCase {

    private final DisputeRepository disputes;
    private final DisputeStatusHistoryRepository history;
    private final OrderRepository orders;
    private final OutboxEventStore outbox;

    public CreateDisputeUseCase(DisputeRepository disputes,
                                DisputeStatusHistoryRepository history,
                                OrderRepository orders,
                                OutboxEventStore outbox) {
        this.disputes = disputes;
        this.history = history;
        this.orders = orders;
        this.outbox = outbox;
    }

    @Transactional
    public DisputeResponse execute(UUID orderId, ActorContext actor, String reason) {
        return execute(orderId, actor, reason, null);
    }

    @Transactional
    public DisputeResponse execute(UUID orderId, ActorContext actor, String reason, String priority) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        TenantAccess.requireParticipant(actor, order.getShopId(), order.getSupplierId(), null);
        return create(order, actor.userId(), reason, priority);
    }

    @Deprecated
    @Transactional
    public DisputeResponse execute(UUID orderId, UUID actorUserId, String actorRole, String reason) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        return create(order, actorUserId, reason, null);
    }

    private DisputeResponse create(Order order, UUID actorUserId, String reason, String priority) {
        UUID orderId = order.getId();
        if (disputes.existsByOrderIdAndStatusIn(orderId, List.of("OPEN", "IN_PROGRESS"))) {
            throw new ConflictException("Un litige actif existe déjà pour cette commande");
        }

        UUID shopId = order.getShopId();
        UUID supplierId = order.getSupplierId();

        Dispute dispute = Dispute.create(orderId, shopId, supplierId, actorUserId, reason,
                DisputePriority.from(priority));
        dispute = disputes.save(dispute);
        history.save(DisputeStatusHistory.record(dispute.getId(), null, dispute.getStatus(),
                actorUserId, "Litige ouvert"));

        String reference = order.getReference();
        outbox.append(new DisputeEvents.DisputeCreatedEvent(
                UUID.randomUUID(), Instant.now(),
                dispute.getId(), orderId, reference,
                shopId, supplierId, actorUserId, reason),
                String.valueOf(dispute.getId()));

        return DisputeResponse.from(dispute);
    }
}
