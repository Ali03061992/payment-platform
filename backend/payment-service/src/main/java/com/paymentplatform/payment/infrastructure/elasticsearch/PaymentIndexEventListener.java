package com.paymentplatform.payment.infrastructure.elasticsearch;

import java.util.UUID;

import com.paymentplatform.shared.domain.event.PaymentEvents;
import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class PaymentIndexEventListener {

    private static final Logger log = LoggerFactory.getLogger(PaymentIndexEventListener.class);

    private final PaymentIndexerService indexer;
    private final PaymentRepository payments;

    public PaymentIndexEventListener(PaymentIndexerService indexer, PaymentRepository payments) {
        this.indexer = indexer;
        this.payments = payments;
    }

    @EventListener
    @Async
    public void onPaymentCreated(PaymentEvents.PaymentCreatedEvent event) {
        indexPayment(event.paymentId());
    }

    @EventListener
    @Async
    public void onPaymentConfirmed(PaymentEvents.PaymentConfirmedEvent event) {
        indexPayment(event.paymentId());
    }

    @EventListener
    @Async
    public void onPaymentRejected(PaymentEvents.PaymentRejectedEvent event) {
        indexPayment(event.paymentId());
    }

    @EventListener
    @Async
    public void onPaymentCancelled(PaymentEvents.PaymentCancelledEvent event) {
        indexPayment(event.paymentId());
    }

    private void indexPayment(UUID paymentId) {
        payments.findById(paymentId).ifPresent(indexer::indexPayment);
    }
}
