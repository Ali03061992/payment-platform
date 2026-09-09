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
        Payment p = payments.save(Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"),
                Money.of(new BigDecimal("100.00"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000005")));

        PaymentEvents.PaymentCreatedEvent event = new PaymentEvents.PaymentCreatedEvent(
                UUID.randomUUID(), java.time.Instant.now(), p.id(),
                p.reference().value(), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), "TND", new BigDecimal("100.00"), UUID.fromString("00000000-0000-0000-0000-000000000005"));

        listener.onPaymentCreated(event);

        assertThat(payments.findById(p.id())).isPresent();
    }

    @Test
    void onPaymentConfirmed_indexesPayment() {
        Payment p = payments.save(Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"),
                Money.of(new BigDecimal("200.00"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000005")));

        PaymentEvents.PaymentConfirmedEvent event = new PaymentEvents.PaymentConfirmedEvent(
                UUID.randomUUID(), java.time.Instant.now(), p.id(),
                p.reference().value(), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000010"));

        listener.onPaymentConfirmed(event);

        assertThat(payments.findById(p.id())).isPresent();
    }

    @Test
    void onPaymentRejected_indexesPayment() {
        Payment p = payments.save(Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"),
                Money.of(new BigDecimal("300.00"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000005")));

        PaymentEvents.PaymentRejectedEvent event = new PaymentEvents.PaymentRejectedEvent(
                UUID.randomUUID(), java.time.Instant.now(), p.id(),
                p.reference().value(), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000010"), "Quality issue");

        listener.onPaymentRejected(event);

        assertThat(payments.findById(p.id())).isPresent();
    }

    @Test
    void onPaymentCancelled_indexesPayment() {
        Payment p = payments.save(Payment.create(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"),
                Money.of(new BigDecimal("400.00"), "TND"), UUID.fromString("00000000-0000-0000-0000-000000000005")));

        PaymentEvents.PaymentCancelledEvent event = new PaymentEvents.PaymentCancelledEvent(
                UUID.randomUUID(), java.time.Instant.now(), p.id(),
                p.reference().value(), UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), UUID.fromString("00000000-0000-0000-0000-000000000005"));

        listener.onPaymentCancelled(event);

        assertThat(payments.findById(p.id())).isPresent();
    }
}
