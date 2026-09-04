package com.paymentplatform.payment.application.usecase;

import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.shared.infrastructure.audit.AuditRecorder;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import com.paymentplatform.shared.domain.event.PaymentEvents.PaymentCreatedEvent;
import com.paymentplatform.payment.application.dto.CreatePaymentRequest;
import com.paymentplatform.payment.application.dto.PaymentNameResolver;
import com.paymentplatform.payment.application.dto.PaymentResponse;
import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.payment.domain.valueobject.Money;
import com.paymentplatform.payment.infrastructure.http.OrganizationValidationClient;
import com.paymentplatform.payment.infrastructure.elasticsearch.PaymentIndexerService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class CreatePaymentUseCase {

    private final PaymentRepository payments;
    private final OrganizationValidationClient orgClient;
    private final PaymentNameResolver nameResolver;
    private final AuditRecorder audit;
    private final OutboxEventStore outbox;
    private final PaymentIndexerService indexer;

    public CreatePaymentUseCase(PaymentRepository payments,
                                OrganizationValidationClient orgClient,
                                PaymentNameResolver nameResolver,
                                AuditRecorder audit, OutboxEventStore outbox,
                                PaymentIndexerService indexer) {
        this.payments = payments;
        this.orgClient = orgClient;
        this.nameResolver = nameResolver;
        this.audit = audit;
        this.outbox = outbox;
        this.indexer = indexer;
    }

    @Transactional
    public PaymentResponse execute(CreatePaymentRequest request, long actorUserId, Long organizationId) {
        orgClient.validateShop(request.shopId());
        orgClient.validateSupplier(request.supplierId());
        orgClient.validateRelation(request.shopId(), request.supplierId());

        Money money = Money.of(request.amount(), request.currency());

        Payment payment = Payment.create(request.shopId(), request.supplierId(), money, actorUserId);

        Payment saved = payments.save(payment);

        audit.record(actorUserId, organizationId, AuditActions.PAYMENT_CREATED,
                saved.id(), "{\"reference\":\"" + saved.reference().value() + "\"}");

        outbox.append(new PaymentCreatedEvent(UUID.randomUUID(), Instant.now(),
                saved.id(), saved.reference().value(), saved.shopId(), saved.supplierId(),
                saved.money().currency(), saved.money().amount(), saved.createdBy()),
                String.valueOf(saved.id()));

        indexer.indexPayment(saved);

        return PaymentResponse.from(saved, nameResolver.toNameResolver());
    }
}
