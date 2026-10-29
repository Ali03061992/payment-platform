package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.payment.application.dto.PaymentNameResolver;
import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.shared.domain.event.PaymentEvents.PaymentCancelledEvent;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CancelPaymentUseCase {

    private final PaymentRepository payments;
    private final AuditRecorder audit;
    private final OutboxEventStore outbox;
    private final PaymentNameResolver nameResolver;

    public CancelPaymentUseCase(PaymentRepository payments, AuditRecorder audit,
                                 OutboxEventStore outbox, PaymentNameResolver nameResolver) {
        this.payments = payments;
        this.audit = audit;
        this.outbox = outbox;
        this.nameResolver = nameResolver;
    }

    @Transactional
    public PaymentResponse execute(UUID id, UUID actorUserId, UUID organizationId) {
        Payment payment = payments.findById(id)
                .orElseThrow(() -> new NotFoundException("Paiement non trouvé : " + id));

        if (organizationId == null || (!payment.belongsToShop(organizationId) && !payment.belongsToSupplier(organizationId))) {
            throw new com.paymentplatform.shared.domain.exception.ForbiddenException(
                    "Vous ne pouvez pas annuler ce paiement");
        }

        return cancel(payment, actorUserId, organizationId);
    }

    @Transactional
    public PaymentResponse executeByOrderId(UUID orderId, UUID actorUserId) {
        Payment payment = payments.findByIdempotencyKey("asap-" + orderId)
                .orElseThrow(() -> new NotFoundException("Paiement auto non trouvé pour la commande : " + orderId));
        if (payment.status() == PaymentStatus.CANCELLED) {
            return PaymentResponse.from(payment, nameResolver.toNameResolver());
        }
        if (payment.status() != PaymentStatus.PENDING) {
            throw new ConflictException("Le paiement auto n'est plus annulable : " + payment.status());
        }
        return cancel(payment, actorUserId, payment.shopId());
    }

    private PaymentResponse cancel(Payment payment, UUID actorUserId, UUID organizationId) {
        UUID id = payment.id();
        payment.cancel(actorUserId);
        Payment saved = payments.save(payment);

        audit.record(actorUserId, organizationId, AuditActions.PAYMENT_CANCELLED,
                id, "{\"reference\":\"" + saved.reference().value() + "\"}");

        outbox.append(new PaymentCancelledEvent(UUID.randomUUID(), Instant.now(),
                saved.id(), saved.reference().value(), saved.shopId(), saved.supplierId(),
                actorUserId), String.valueOf(saved.id()));

        return PaymentResponse.from(saved, nameResolver.toNameResolver());
    }
}
