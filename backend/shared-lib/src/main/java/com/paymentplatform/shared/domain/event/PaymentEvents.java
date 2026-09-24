package com.paymentplatform.shared.domain.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Événements du bounded context Payment. */
public final class PaymentEvents {

    private PaymentEvents() {
    }

    public record PaymentCreatedEvent(UUID eventId, Instant occurredAt, UUID paymentId, String reference,
                                      UUID shopId, UUID supplierId, String currency, BigDecimal amount,
                                      UUID createdBy) implements DomainEvent {
        public static final String EVENT_TYPE = "payment.created";

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateId() {
            return String.valueOf(paymentId);
        }
    }

    public record PaymentConfirmedEvent(UUID eventId, Instant occurredAt, UUID paymentId, String reference,
                                        UUID shopId, UUID supplierId, UUID confirmedBy,
                                        BigDecimal amount, String currency) implements DomainEvent {
        public static final String EVENT_TYPE = "payment.confirmed";

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateId() {
            return String.valueOf(paymentId);
        }
    }

    public record PaymentRejectedEvent(UUID eventId, Instant occurredAt, UUID paymentId, String reference,
                                       UUID shopId, UUID supplierId, UUID rejectedBy, String rejectionReason)
            implements DomainEvent {
        public static final String EVENT_TYPE = "payment.rejected";

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateId() {
            return String.valueOf(paymentId);
        }
    }

    public record PaymentCancelledEvent(UUID eventId, Instant occurredAt, UUID paymentId, String reference,
                                        UUID shopId, UUID supplierId, UUID cancelledBy) implements DomainEvent {
        public static final String EVENT_TYPE = "payment.cancelled";

        @Override
        public String eventType() {
            return EVENT_TYPE;
        }

        @Override
        public int eventVersion() {
            return 1;
        }

        @Override
        public String aggregateId() {
            return String.valueOf(paymentId);
        }
    }
}