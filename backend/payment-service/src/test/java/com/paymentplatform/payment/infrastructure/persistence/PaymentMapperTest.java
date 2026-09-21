package com.paymentplatform.payment.infrastructure.persistence;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.valueobject.Money;
import com.paymentplatform.payment.domain.valueobject.RejectionReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper();

    @Test
    void toJpa_mapsAllFields() {
        Payment payment = Payment.create(
                UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"),
                Money.of(new BigDecimal("100.50"), "TND"),
                UUID.fromString("00000000-0000-0000-0000-000000000005"));

        var jpa = mapper.toJpa(payment);

        assertThat(jpa.getReference()).isEqualTo(payment.reference().value());
        assertThat(jpa.getShopId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(jpa.getSupplierId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        assertThat(jpa.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(jpa.getCurrency()).isEqualTo("TND");
        assertThat(jpa.getStatus()).isEqualTo(PaymentStatus.PENDING.name());
        assertThat(jpa.getCreatedBy()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000005"));
        assertThat(jpa.getCreatedAt()).isNotNull();
    }

    @Test
    void toJpa_withRejectionReason() {
        Payment payment = Payment.create(
                UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"),
                Money.of(new BigDecimal("200.00"), "TND"),
                UUID.fromString("00000000-0000-0000-0000-000000000005"));
        Payment rejected = payment.reject(UUID.fromString("00000000-0000-0000-0000-000000000010"), new RejectionReason("Quality issue"));

        var jpa = mapper.toJpa(rejected);

        assertThat(jpa.getRejectionReason()).isEqualTo("Quality issue");
        assertThat(jpa.getStatus()).isEqualTo(PaymentStatus.REJECTED.name());
    }

    @Test
    void toEventJpa_mapsAllFields() {
        var event = new com.paymentplatform.payment.domain.model.PaymentEvent(
                UUID.fromString("00000000-0000-0000-0000-000000000000"), UUID.fromString("00000000-0000-0000-0000-000000000001"), "PAYMENT_CONFIRMED", UUID.fromString("00000000-0000-0000-0000-000000000010"), Instant.now(), "Confirmed");

        var eventJpa = mapper.toEventJpa(event, UUID.fromString("00000000-0000-0000-0000-000000000001"));

        assertThat(eventJpa.getPaymentId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(eventJpa.getAction()).isEqualTo("PAYMENT_CONFIRMED");
        assertThat(eventJpa.getUserId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(eventJpa.getDetails()).isEqualTo("Confirmed");
    }

    @Test
    void toEventDomain_mapsAllFields() {
        var eventJpa = new PaymentEventJpaEntity();
        eventJpa.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        eventJpa.setPaymentId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        eventJpa.setAction("PAYMENT_CREATED");
        eventJpa.setUserId(UUID.fromString("00000000-0000-0000-0000-000000000005"));
        eventJpa.setTimestamp(Instant.now());
        eventJpa.setDetails("Created");

        var domain = mapper.toEventDomain(eventJpa);

        assertThat(domain.id()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(domain.paymentId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(domain.action()).isEqualTo("PAYMENT_CREATED");
        assertThat(domain.userId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000005"));
        assertThat(domain.details()).isEqualTo("Created");
    }

    @Test
    void fromFields_reconstructsPayment() {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        entity.setReference("PAY-003");
        entity.setShopId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        entity.setSupplierId(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        entity.setAmount(new BigDecimal("300.00"));
        entity.setCurrency("TND");
        entity.setStatus("PENDING");
        entity.setCreatedBy(UUID.fromString("00000000-0000-0000-0000-000000000005"));
        entity.setVersion(0L);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        var payment = mapper.fromFields(entity, java.util.List.of());

        assertThat(payment.id()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        assertThat(payment.reference().value()).isEqualTo("PAY-003");
        assertThat(payment.shopId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(payment.supplierId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        assertThat(payment.money().amount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void fromFields_withRejectionReason() {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(UUID.fromString("00000000-0000-0000-0000-000000000002"));
        entity.setReference("PAY-004");
        entity.setShopId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        entity.setSupplierId(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        entity.setAmount(new BigDecimal("400.00"));
        entity.setCurrency("TND");
        entity.setStatus("REJECTED");
        entity.setRejectionReason("Quality issue");
        entity.setCreatedBy(UUID.fromString("00000000-0000-0000-0000-000000000005"));
        entity.setVersion(0L);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        var payment = mapper.fromFields(entity, java.util.List.of());

        assertThat(payment.rejectionReason()).isNotNull();
        assertThat(payment.rejectionReason().value()).isEqualTo("Quality issue");
    }
}
