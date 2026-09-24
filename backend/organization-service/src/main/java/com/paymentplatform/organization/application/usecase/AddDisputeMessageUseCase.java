package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.DisputeResponse;
import com.paymentplatform.organization.domain.model.Dispute;
import com.paymentplatform.organization.domain.model.DisputeMessage;
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
public class AddDisputeMessageUseCase {

    private final DisputeRepository disputes;
    private final OutboxEventStore outbox;

    public AddDisputeMessageUseCase(DisputeRepository disputes, OutboxEventStore outbox) {
        this.disputes = disputes;
        this.outbox = outbox;
    }

    @Transactional
    public DisputeResponse execute(UUID disputeId, UUID actorUserId, String actorRole, String content) {
        Dispute dispute = disputes.findById(disputeId)
                .orElseThrow(() -> new NotFoundException("Litige non trouvé : " + disputeId));

        if ("CLOSED".equals(dispute.getStatus())) {
            throw new ConflictException("Impossible d'ajouter un message à un litige fermé");
        }

        if ("OPEN".equals(dispute.getStatus())) {
            dispute.transitionTo(com.paymentplatform.organization.domain.model.DisputeStatus.IN_PROGRESS);
        }

        DisputeMessage message = DisputeMessage.create(disputeId, actorUserId, actorRole, content);
        dispute.addMessage(message);
        dispute = disputes.save(dispute);

        outbox.append(new DisputeEvents.DisputeMessageAddedEvent(
                UUID.randomUUID(), Instant.now(),
                disputeId, dispute.getOrderId(),
                dispute.getShopId(), dispute.getSupplierId(),
                actorUserId, actorRole, content),
                String.valueOf(disputeId));

        return DisputeResponse.from(dispute);
    }
}
