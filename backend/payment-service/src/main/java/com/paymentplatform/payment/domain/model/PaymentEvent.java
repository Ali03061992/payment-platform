package com.paymentplatform.payment.domain.model;

import java.util.UUID;

import java.time.Instant;

public record PaymentEvent(UUID id, UUID paymentId, String action, UUID userId, Instant timestamp, String details) {

    public static PaymentEvent create(UUID paymentId, String action, UUID userId, String details) {
        return new PaymentEvent(null, paymentId, action, userId, Instant.now(), details);
    }
}
