package com.paymentplatform.payment.application.dto;

import com.paymentplatform.payment.domain.model.Payment;
import com.paymentplatform.payment.domain.model.PaymentEvent;
import com.paymentplatform.payment.domain.model.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String reference,
        UUID shopId,
        String shopName,
        UUID supplierId,
        String supplierName,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String rejectionReason,
        UUID createdBy,
        String createdByName,
        String confirmedByName,
        String rejectedByName,
        String cancelledByName,
        UUID orderId,
        LocalDate dueDate,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<PaymentEventResponse> events
) {
    public static PaymentResponse from(Payment payment, NameResolver names) {
        String shopName = names.resolveOrg(payment.shopId());
        String supplierName = names.resolveOrg(payment.supplierId());
        String createdByName = names.resolveUser(payment.createdBy());

        String confirmedByName = null;
        String rejectedByName = null;
        String cancelledByName = null;
        for (PaymentEvent event : payment.events()) {
            String userName = event.userId() != null ? names.resolveUser(event.userId()) : null;
            switch (event.action()) {
                case "PAYMENT_CONFIRMED" -> confirmedByName = userName;
                case "PAYMENT_REJECTED" -> rejectedByName = userName;
                case "PAYMENT_CANCELLED" -> cancelledByName = userName;
            }
        }

        return new PaymentResponse(
                payment.id(),
                payment.reference().value(),
                payment.shopId(),
                shopName,
                payment.supplierId(),
                supplierName,
                payment.money().amount(),
                payment.money().currency(),
                payment.status(),
                payment.rejectionReason() != null ? payment.rejectionReason().value() : null,
                payment.createdBy(),
                createdByName,
                confirmedByName,
                rejectedByName,
                cancelledByName,
                payment.orderId(),
                payment.dueDate(),
                payment.version(),
                payment.createdAt(),
                payment.updatedAt(),
                payment.events().stream().map(e -> PaymentEventResponse.from(e, names)).toList()
        );
    }

    public record PaymentEventResponse(String action, UUID userId, String userName, Instant timestamp, String details) {
        public static PaymentEventResponse from(PaymentEvent event, NameResolver names) {
            String userName = event.userId() != null ? names.resolveUser(event.userId()) : null;
            return new PaymentEventResponse(event.action(), event.userId(), userName, event.timestamp(), event.details());
        }
    }

    public interface NameResolver {
        String resolveOrg(UUID organizationId);
        String resolveUser(UUID userId);
    }
}
