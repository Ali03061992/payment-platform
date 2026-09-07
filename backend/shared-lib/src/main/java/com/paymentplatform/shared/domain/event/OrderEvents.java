package com.paymentplatform.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Événements du bounded context Organization (commandes). */
public final class OrderEvents {

    private OrderEvents() {
    }

    public record OrderCreatedEvent(UUID eventId, Instant occurredAt, long orderId, String reference,
                                    long shopId, long supplierId, long createdBy, String source)
            implements DomainEvent {
        public static final String EVENT_TYPE = "order.created";

        @Override public String eventType() { return EVENT_TYPE; }
        @Override public int eventVersion() { return 1; }
        @Override public String aggregateId() { return String.valueOf(orderId); }
    }

    public record OrderConfirmedEvent(UUID eventId, Instant occurredAt, long orderId, String reference,
                                      long shopId, long supplierId, long confirmedBy) implements DomainEvent {
        public static final String EVENT_TYPE = "order.confirmed";

        @Override public String eventType() { return EVENT_TYPE; }
        @Override public int eventVersion() { return 1; }
        @Override public String aggregateId() { return String.valueOf(orderId); }
    }

    public record OrderPreparingEvent(UUID eventId, Instant occurredAt, long orderId, String reference,
                                      long shopId, long supplierId, long startedBy) implements DomainEvent {
        public static final String EVENT_TYPE = "order.preparing";

        @Override public String eventType() { return EVENT_TYPE; }
        @Override public int eventVersion() { return 1; }
        @Override public String aggregateId() { return String.valueOf(orderId); }
    }

    public record OrderReadyForDeliveryEvent(UUID eventId, Instant occurredAt, long orderId, String reference,
                                             long shopId, long supplierId, long readyBy) implements DomainEvent {
        public static final String EVENT_TYPE = "order.ready_for_delivery";

        @Override public String eventType() { return EVENT_TYPE; }
        @Override public int eventVersion() { return 1; }
        @Override public String aggregateId() { return String.valueOf(orderId); }
    }

    public record OrderDeliveredEvent(UUID eventId, Instant occurredAt, long orderId, String reference,
                                      long shopId, long supplierId, long deliveredBy, long receivedBy)
            implements DomainEvent {
        public static final String EVENT_TYPE = "order.delivered";

        @Override public String eventType() { return EVENT_TYPE; }
        @Override public int eventVersion() { return 1; }
        @Override public String aggregateId() { return String.valueOf(orderId); }
    }

    public record OrderAcceptedEvent(UUID eventId, Instant occurredAt, long orderId, String reference,
                                     long shopId, long supplierId, long acceptedBy) implements DomainEvent {
        public static final String EVENT_TYPE = "order.accepted";

        @Override public String eventType() { return EVENT_TYPE; }
        @Override public int eventVersion() { return 1; }
        @Override public String aggregateId() { return String.valueOf(orderId); }
    }

    public record OrderCancelledEvent(UUID eventId, Instant occurredAt, long orderId, String reference,
                                      long shopId, long supplierId, long cancelledBy) implements DomainEvent {
        public static final String EVENT_TYPE = "order.cancelled";

        @Override public String eventType() { return EVENT_TYPE; }
        @Override public int eventVersion() { return 1; }
        @Override public String aggregateId() { return String.valueOf(orderId); }
    }

    public record OrderRejectedEvent(UUID eventId, Instant occurredAt, long orderId, String reference,
                                     long shopId, long supplierId, long rejectedBy) implements DomainEvent {
        public static final String EVENT_TYPE = "order.rejected";

        @Override public String eventType() { return EVENT_TYPE; }
        @Override public int eventVersion() { return 1; }
        @Override public String aggregateId() { return String.valueOf(orderId); }
    }

    public record OrderDeliveryRejectedEvent(UUID eventId, Instant occurredAt, long orderId, String reference,
                                              long shopId, long supplierId, long rejectedBy, String reason)
            implements DomainEvent {
        public static final String EVENT_TYPE = "order.delivery_rejected";

        @Override public String eventType() { return EVENT_TYPE; }
        @Override public int eventVersion() { return 1; }
        @Override public String aggregateId() { return String.valueOf(orderId); }
    }

    public record LowStockAlertEvent(UUID eventId, Instant occurredAt, long productId, String productName,
                                      String sku, int availableQty, int minQuantity, long supplierId)
            implements DomainEvent {
        public static final String EVENT_TYPE = "order.low_stock_alert";

        @Override public String eventType() { return EVENT_TYPE; }
        @Override public int eventVersion() { return 1; }
        @Override public String aggregateId() { return String.valueOf(productId); }
    }
}
