package com.paymentplatform.payment.domain.model;

import java.time.Instant;

public record PaymentEvent(long id, long paymentId, String action, Long userId, Instant timestamp, String details) {

    public static PaymentEvent create(long paymentId, String action, Long userId, String details) {
        return new PaymentEvent(0, paymentId, action, userId, Instant.now(), details);
    }
}
