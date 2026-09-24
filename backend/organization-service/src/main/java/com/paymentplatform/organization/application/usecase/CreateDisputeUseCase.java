package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.DisputeResponse;
import com.paymentplatform.organization.domain.model.Dispute;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.repository.DisputeRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.shared.domain.event.DisputeEvents;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CreateDisputeUseCase {

    private final DisputeRepository disputes;
    private final OrderRepository orders;
    private final OutboxEventStore outbox;

    public CreateDisputeUseCase(DisputeRepository disputes, OrderRepository orders, OutboxEventStore outbox) {
        this.disputes = disputes;
        this.orders = orders;
        this.outbox = outbox;
    }

    @Transactional
    public DisputeResponse execute(UUID orderId, UUID actorUserId, String actorRole, String reason) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        String status = order.getStatus();
        if (!"REJECTED".equals(status) && !"DELIVERY_REJECTED".equals(status)) {
            throw new ConflictException("Un litige ne peut être ouvert que pour une commande rejetée (REJECTED ou DELIVERY_REJECTED)");
        }

        UUID shopId = order.getShopId();
        UUID supplierId = order.getSupplierId();

        Dispute dispute = Dispute.create(orderId, shopId, supplierId, actorUserId, reason);
        dispute = disputes.save(dispute);

        String reference = order.getReference();
        outbox.append(new DisputeEvents.DisputeCreatedEvent(
                UUID.randomUUID(), Instant.now(),
                dispute.getId(), orderId, reference,
                shopId, supplierId, actorUserId, reason),
                String.valueOf(dispute.getId()));

        return DisputeResponse.from(dispute);
    }
}
