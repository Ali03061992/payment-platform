package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import com.paymentplatform.shared.domain.event.PaymentEvents.PaymentRejectedEvent;
import com.paymentplatform.payment.application.dto.PaymentNameResolver;
import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.application.dto.RejectPaymentRequest;
import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.payment.domain.valueobject.RejectionReason;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class RejectPaymentUseCase {

    private final PaymentRepository payments;
    private final AuditRecorder audit;
    private final OutboxEventStore outbox;
    private final PaymentNameResolver nameResolver;

    public RejectPaymentUseCase(PaymentRepository payments, AuditRecorder audit,
                                 OutboxEventStore outbox, PaymentNameResolver nameResolver) {
        this.payments = payments;
        this.audit = audit;
        this.outbox = outbox;
        this.nameResolver = nameResolver;
    }

    @Transactional
    public PaymentResponse execute(UUID id, RejectPaymentRequest request, UUID actorUserId, UUID organizationId) {
        Payment payment = payments.findById(id)
                .orElseThrow(() -> new NotFoundException("Paiement non trouvé : " + id));

        if (organizationId != null && !payment.belongsToSupplier(organizationId)) {
            throw new com.paymentplatform.shared.domain.exception.ForbiddenException(
                    "Vous ne pouvez pas refuser ce paiement");
        }

        RejectionReason reason = new RejectionReason(request.rejectionReason());
        payment.reject(actorUserId, reason);
        Payment saved = payments.save(payment);

        audit.record(actorUserId, organizationId, AuditActions.PAYMENT_REJECTED,
                id, "{\"reference\":\"" + saved.reference().value()
                        + "\",\"reason\":\"" + reason.value() + "\"}");

        outbox.append(new PaymentRejectedEvent(UUID.randomUUID(), Instant.now(),
                saved.id(), saved.reference().value(), saved.shopId(), saved.supplierId(),
                actorUserId, reason.value()), String.valueOf(saved.id()));

        return PaymentResponse.from(saved, nameResolver.toNameResolver());
    }
}
