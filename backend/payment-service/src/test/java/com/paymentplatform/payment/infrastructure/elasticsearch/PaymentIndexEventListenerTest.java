package com.paymentplatform.payment.infrastructure.elasticsearch;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import com.paymentplatform.payment.domain.valueobject.Money;
import com.paymentplatform.shared.domain.event.PaymentEvents;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentIndexEventListenerTest {

    @Autowired private PaymentIndexEventListener listener;
    @Autowired private PaymentIndexerService indexerService;
    @Autowired private PaymentRepository payments;

    @Test
    void onPaymentCreated_indexesPayment() {
        Payment p = payments.save(Payment.create(10L, 20L,
                Money.of(new BigDecimal("100.00"), "TND"), 5L));

        PaymentEvents.PaymentCreatedEvent event = new PaymentEvents.PaymentCreatedEvent(
                UUID.randomUUID(), java.time.Instant.now(), p.id(),
                p.reference().value(), 10L, 20L, "TND", new BigDecimal("100.00"), 5L);

        listener.onPaymentCreated(event);

        assertThat(payments.findById(p.id())).isPresent();
    }

    @Test
    void onPaymentConfirmed_indexesPayment() {
        Payment p = payments.save(Payment.create(10L, 20L,
                Money.of(new BigDecimal("200.00"), "TND"), 5L));

        PaymentEvents.PaymentConfirmedEvent event = new PaymentEvents.PaymentConfirmedEvent(
                UUID.randomUUID(), java.time.Instant.now(), p.id(),
                p.reference().value(), 10L, 20L, 10L);

        listener.onPaymentConfirmed(event);

        assertThat(payments.findById(p.id())).isPresent();
    }

    @Test
    void onPaymentRejected_indexesPayment() {
        Payment p = payments.save(Payment.create(10L, 20L,
                Money.of(new BigDecimal("300.00"), "TND"), 5L));

        PaymentEvents.PaymentRejectedEvent event = new PaymentEvents.PaymentRejectedEvent(
                UUID.randomUUID(), java.time.Instant.now(), p.id(),
                p.reference().value(), 10L, 20L, 10L, "Quality issue");

        listener.onPaymentRejected(event);

        assertThat(payments.findById(p.id())).isPresent();
    }

    @Test
    void onPaymentCancelled_indexesPayment() {
        Payment p = payments.save(Payment.create(10L, 20L,
                Money.of(new BigDecimal("400.00"), "TND"), 5L));

        PaymentEvents.PaymentCancelledEvent event = new PaymentEvents.PaymentCancelledEvent(
                UUID.randomUUID(), java.time.Instant.now(), p.id(),
                p.reference().value(), 10L, 20L, 5L);

        listener.onPaymentCancelled(event);

        assertThat(payments.findById(p.id())).isPresent();
    }
}
