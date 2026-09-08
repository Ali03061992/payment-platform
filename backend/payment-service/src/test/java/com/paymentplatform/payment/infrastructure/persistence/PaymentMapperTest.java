package com.paymentplatform.payment.infrastructure.persistence;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentStatus;
import com.paymentplatform.payment.domain.valueobject.Money;
import com.paymentplatform.payment.domain.valueobject.RejectionReason;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentMapperTest {

    private final PaymentMapper mapper = new PaymentMapper();

    @Test
    void toJpa_mapsAllFields() {
        Payment payment = Payment.create(
                10L, 20L,
                Money.of(new BigDecimal("100.50"), "TND"),
                5L);

        var jpa = mapper.toJpa(payment);

        assertThat(jpa.getReference()).isEqualTo(payment.reference().value());
        assertThat(jpa.getShopId()).isEqualTo(10L);
        assertThat(jpa.getSupplierId()).isEqualTo(20L);
        assertThat(jpa.getAmount()).isEqualByComparingTo(new BigDecimal("100.50"));
        assertThat(jpa.getCurrency()).isEqualTo("TND");
        assertThat(jpa.getStatus()).isEqualTo(PaymentStatus.PENDING.name());
        assertThat(jpa.getCreatedBy()).isEqualTo(5L);
        assertThat(jpa.getCreatedAt()).isNotNull();
    }

    @Test
    void toJpa_withRejectionReason() {
        Payment payment = Payment.create(
                10L, 20L,
                Money.of(new BigDecimal("200.00"), "TND"),
                5L);
        Payment rejected = payment.reject(10L, new RejectionReason("Quality issue"));

        var jpa = mapper.toJpa(rejected);

        assertThat(jpa.getRejectionReason()).isEqualTo("Quality issue");
        assertThat(jpa.getStatus()).isEqualTo(PaymentStatus.REJECTED.name());
    }

    @Test
    void toEventJpa_mapsAllFields() {
        var event = new com.paymentplatform.payment.domain.model.PaymentEvent(
                0L, 1L, "PAYMENT_CONFIRMED", 10L, Instant.now(), "Confirmed");

        var eventJpa = mapper.toEventJpa(event, 1L);

        assertThat(eventJpa.getPaymentId()).isEqualTo(1L);
        assertThat(eventJpa.getAction()).isEqualTo("PAYMENT_CONFIRMED");
        assertThat(eventJpa.getUserId()).isEqualTo(10L);
        assertThat(eventJpa.getDetails()).isEqualTo("Confirmed");
    }

    @Test
    void toEventDomain_mapsAllFields() {
        var eventJpa = new PaymentEventJpaEntity();
        eventJpa.setId(1L);
        eventJpa.setPaymentId(1L);
        eventJpa.setAction("PAYMENT_CREATED");
        eventJpa.setUserId(5L);
        eventJpa.setTimestamp(Instant.now());
        eventJpa.setDetails("Created");

        var domain = mapper.toEventDomain(eventJpa);

        assertThat(domain.id()).isEqualTo(1L);
        assertThat(domain.paymentId()).isEqualTo(1L);
        assertThat(domain.action()).isEqualTo("PAYMENT_CREATED");
        assertThat(domain.userId()).isEqualTo(5L);
        assertThat(domain.details()).isEqualTo("Created");
    }

    @Test
    void fromFields_reconstructsPayment() {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(1L);
        entity.setReference("PAY-003");
        entity.setShopId(10L);
        entity.setSupplierId(20L);
        entity.setAmount(new BigDecimal("300.00"));
        entity.setCurrency("TND");
        entity.setStatus("PENDING");
        entity.setCreatedBy(5L);
        entity.setVersion(0L);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        var payment = mapper.fromFields(entity, java.util.List.of());

        assertThat(payment.id()).isEqualTo(1L);
        assertThat(payment.reference().value()).isEqualTo("PAY-003");
        assertThat(payment.shopId()).isEqualTo(10L);
        assertThat(payment.supplierId()).isEqualTo(20L);
        assertThat(payment.money().amount()).isEqualByComparingTo(new BigDecimal("300.00"));
        assertThat(payment.status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void fromFields_withRejectionReason() {
        PaymentJpaEntity entity = new PaymentJpaEntity();
        entity.setId(2L);
        entity.setReference("PAY-004");
        entity.setShopId(10L);
        entity.setSupplierId(20L);
        entity.setAmount(new BigDecimal("400.00"));
        entity.setCurrency("TND");
        entity.setStatus("REJECTED");
        entity.setRejectionReason("Quality issue");
        entity.setCreatedBy(5L);
        entity.setVersion(0L);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        var payment = mapper.fromFields(entity, java.util.List.of());

        assertThat(payment.rejectionReason()).isNotNull();
        assertThat(payment.rejectionReason().value()).isEqualTo("Quality issue");
    }
}
