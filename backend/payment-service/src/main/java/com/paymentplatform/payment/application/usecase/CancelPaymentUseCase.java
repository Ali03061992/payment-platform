package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import com.paymentplatform.shared.domain.event.PaymentEvents.PaymentCancelledEvent;
import com.paymentplatform.payment.application.dto.PaymentNameResolver;
import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.payment.infrastructure.elasticsearch.PaymentIndexerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CancelPaymentUseCase {

    private final PaymentRepository payments;
    private final AuditRecorder audit;
    private final OutboxEventStore outbox;
    private final PaymentIndexerService indexer;
    private final PaymentNameResolver nameResolver;

    public CancelPaymentUseCase(PaymentRepository payments, AuditRecorder audit,
                                 OutboxEventStore outbox, PaymentIndexerService indexer,
                                 PaymentNameResolver nameResolver) {
        this.payments = payments;
        this.audit = audit;
        this.outbox = outbox;
        this.indexer = indexer;
        this.nameResolver = nameResolver;
    }

    @Transactional
    public PaymentResponse execute(long id, long actorUserId, Long organizationId) {
        Payment payment = payments.findById(id)
                .orElseThrow(() -> new NotFoundException("Paiement non trouvé : " + id));

        if (organizationId != null && !payment.belongsToShop(organizationId) && !payment.belongsToSupplier(organizationId)) {
            throw new com.paymentplatform.shared.domain.exception.ForbiddenException(
                    "Vous ne pouvez pas annuler ce paiement");
        }

        payment.cancel(actorUserId);
        Payment saved = payments.save(payment);

        audit.record(actorUserId, organizationId, AuditActions.PAYMENT_CANCELLED,
                id, "{\"reference\":\"" + saved.reference().value() + "\"}");

        outbox.append(new PaymentCancelledEvent(UUID.randomUUID(), Instant.now(),
                saved.id(), saved.reference().value(), saved.shopId(), saved.supplierId(),
                actorUserId), String.valueOf(saved.id()));

        indexer.indexPayment(saved);

        return PaymentResponse.from(saved, nameResolver.toNameResolver());
    }
}
