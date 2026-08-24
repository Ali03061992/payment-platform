package com.paymentplatform.payment.application.dto;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record PaymentResponse(
        Long id,
        String reference,
        long shopId,
        long supplierId,
        BigDecimal amount,
        String currency,
        String status,
        String rejectionReason,
        long createdBy,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<PaymentEventResponse> events
) {
    public static PaymentResponse from(Payment payment) {
        return new PaymentResponse(
                payment.id(),
                payment.reference().value(),
                payment.shopId(),
                payment.supplierId(),
                payment.money().amount(),
                payment.money().currency(),
                payment.status().name(),
                payment.rejectionReason() != null ? payment.rejectionReason().value() : null,
                payment.createdBy(),
                payment.version(),
                payment.createdAt(),
                payment.updatedAt(),
                payment.events().stream().map(PaymentEventResponse::from).toList()
        );
    }

    public record PaymentEventResponse(String action, Long userId, Instant timestamp, String details) {
        public static PaymentEventResponse from(PaymentEvent event) {
            return new PaymentEventResponse(event.action(), event.userId(), event.timestamp(), event.details());
        }
    }
}
