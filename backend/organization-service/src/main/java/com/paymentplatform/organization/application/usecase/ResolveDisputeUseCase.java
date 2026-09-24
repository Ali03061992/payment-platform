package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.DisputeResponse;
import com.paymentplatform.organization.domain.model.Dispute;
import com.paymentplatform.organization.domain.model.DisputeStatus;
import com.paymentplatform.organization.domain.repository.DisputeRepository;
import com.paymentplatform.shared.domain.event.DisputeEvents;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
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
    public DisputeResponse execute(UUID disputeId, UUID actorUserId, String targetStatus) {
        Dispute dispute = disputes.findById(disputeId)
                .orElseThrow(() -> new NotFoundException("Litige non trouvé : " + disputeId));

        DisputeStatus newStatus = DisputeStatus.valueOf(targetStatus);
        if (newStatus != DisputeStatus.RESOLVED && newStatus != DisputeStatus.CLOSED) {
            throw new ConflictException("Seul RESOLVED ou CLOSED sont autorisés comme statut de résolution");
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
