package com.paymentplatform.payment.infrastructure.persistence;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentEvent;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.valueobject.Money;
import com.paymentplatform.payment.domain.valueobject.PaymentReference;
import com.paymentplatform.payment.domain.valueobject.RejectionReason;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class PaymentMapper {

    public PaymentJpaEntity toJpa(Payment payment) {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(payment.id());
        entity.setReference(payment.reference().value());
        entity.setShopId(payment.shopId());
        entity.setSupplierId(payment.supplierId());
        entity.setCurrency(payment.money().currency());
        entity.setAmount(payment.money().amount());
        entity.setStatus(payment.status().name());
        entity.setRejectionReason(payment.rejectionReason() != null ? payment.rejectionReason().value() : null);
        entity.setCreatedBy(payment.createdBy());
        entity.setVersion(payment.version());
        entity.setCreatedAt(payment.createdAt());
        entity.setUpdatedAt(payment.updatedAt());
        return entity;
    }

    public Payment fromFields(PaymentJpaEntity entity, List<PaymentEvent> events) {
        try {
            var ctor = Payment.class.getDeclaredConstructor(
                    UUID.class, PaymentReference.class, UUID.class, UUID.class,
                    Money.class, PaymentStatus.class, RejectionReason.class,
                    UUID.class, java.time.Instant.class, java.time.Instant.class,
                    long.class, List.class);
            ctor.setAccessible(true);
            return ctor.newInstance(
                    entity.getId(),
                    new PaymentReference(entity.getReference()),
                    entity.getShopId(),
                    entity.getSupplierId(),
                    Money.of(entity.getAmount(), entity.getCurrency()),
                    PaymentStatus.from(entity.getStatus()),
                    entity.getRejectionReason() != null ? new RejectionReason(entity.getRejectionReason()) : null,
                    entity.getCreatedBy(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt(),
                    entity.getVersion() != null ? entity.getVersion() : 0L,
                    events
            );
        } catch (Exception e) {
            throw new RuntimeException("Erreur de mapping Payment", e);
        }
    }

    public PaymentEventJpaEntity toEventJpa(PaymentEvent event, UUID paymentId) {
        PaymentEventJpaEntity entity = new PaymentEventJpaEntity();
        entity.setPaymentId(paymentId);
        entity.setAction(event.action());
        entity.setUserId(event.userId());
        entity.setTimestamp(event.timestamp());
        entity.setDetails(event.details());
        return entity;
    }

    public PaymentEvent toEventDomain(PaymentEventJpaEntity entity) {
        return new PaymentEvent(entity.getId(), entity.getPaymentId(), entity.getAction(),
                entity.getUserId(), entity.getTimestamp(), entity.getDetails());
    }
}
