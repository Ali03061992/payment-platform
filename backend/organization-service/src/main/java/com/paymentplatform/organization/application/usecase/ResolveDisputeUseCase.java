package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.DisputeResponse;
import com.paymentplatform.organization.application.security.TenantAccess;
import com.paymentplatform.organization.domain.model.Dispute;
import com.paymentplatform.organization.domain.model.DisputeStatus;
import com.paymentplatform.organization.domain.repository.DisputeRepository;
import com.paymentplatform.shared.domain.event.DisputeEvents;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.security.ActorContext;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ResolveDisputeUseCase {

    private final DisputeRepository disputes;
    private final OutboxEventStore outbox;

    public ResolveDisputeUseCase(DisputeRepository disputes, OutboxEventStore outbox) {
        this.disputes = disputes;
        this.outbox = outbox;
    }

    @Transactional
    public DisputeResponse execute(UUID disputeId, ActorContext actor, String targetStatus) {
        Dispute dispute = disputes.findById(disputeId)
                .orElseThrow(() -> new NotFoundException("Litige non trouvé : " + disputeId));
        TenantAccess.requireParticipant(actor, dispute.getShopId(), dispute.getSupplierId(), null);
        return resolve(dispute, actor.userId(), targetStatus);
    }

    @Deprecated
    @Transactional
    public DisputeResponse execute(UUID disputeId, UUID actorUserId, String targetStatus) {
        Dispute dispute = disputes.findById(disputeId)
                .orElseThrow(() -> new NotFoundException("Litige non trouvé : " + disputeId));
        return resolve(dispute, actorUserId, targetStatus);
    }

    private DisputeResponse resolve(Dispute dispute, UUID actorUserId, String targetStatus) {
        UUID disputeId = dispute.getId();
        DisputeStatus newStatus = DisputeStatus.valueOf(targetStatus);
        if (newStatus != DisputeStatus.RESOLVED && newStatus != DisputeStatus.CLOSED) {
            throw new ConflictException("Seul RESOLVED ou CLOSED sont autorisés comme statut de résolution");
        }

        if (newStatus == DisputeStatus.RESOLVED && DisputeStatus.OPEN.name().equals(dispute.getStatus())) {
            dispute.transitionTo(DisputeStatus.IN_PROGRESS);
        }
        dispute.transitionTo(newStatus);
        dispute = disputes.save(dispute);

        String eventType = newStatus == DisputeStatus.RESOLVED
                ? DisputeEvents.DisputeResolvedEvent.EVENT_TYPE
                : DisputeEvents.DisputeClosedEvent.EVENT_TYPE;

        if (newStatus == DisputeStatus.RESOLVED) {
            outbox.append(new DisputeEvents.DisputeResolvedEvent(
                    UUID.randomUUID(), Instant.now(),
                    disputeId, dispute.getOrderId(),
                    dispute.getShopId(), dispute.getSupplierId(),
                    actorUserId),
                    String.valueOf(disputeId));
        } else {
            outbox.append(new DisputeEvents.DisputeClosedEvent(
                    UUID.randomUUID(), Instant.now(),
                    disputeId, dispute.getOrderId(),
                    dispute.getShopId(), dispute.getSupplierId(),
                    actorUserId),
                    String.valueOf(disputeId));
        }

        return DisputeResponse.from(dispute);
    }
}
