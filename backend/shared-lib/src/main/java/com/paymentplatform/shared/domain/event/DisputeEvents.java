package com.paymentplatform.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Événements du bounded context Organization (litiges). */
public final class DisputeEvents {

    private DisputeEvents() {
    }

    public record DisputeCreatedEvent(UUID eventId, Instant occurredAt,
                                       UUID disputeId, UUID orderId, String reference,
                                       UUID shopId, UUID supplierId, UUID openedBy, String reason)
            implements DomainEvent {
        public static final String EVENT_TYPE = "dispute.created";

        @Override public String eventType() { return EVENT_TYPE; }
        @Override public int eventVersion() { return 1; }
        @Override public String aggregateId() { return String.valueOf(disputeId); }
    }

    public record DisputeMessageAddedEvent(UUID eventId, Instant occurredAt,
                                            UUID disputeId, UUID orderId,
                                            UUID shopId, UUID supplierId,
                                            UUID senderId, String senderRole, String content)
            implements DomainEvent {
        public static final String EVENT_TYPE = "dispute.message_added";

        @Override public String eventType() { return EVENT_TYPE; }
        @Override public int eventVersion() { return 1; }
        @Override public String aggregateId() { return String.valueOf(disputeId); }
    }

    public record DisputeResolvedEvent(UUID eventId, Instant occurredAt,
                                        UUID disputeId, UUID orderId,
                                        UUID shopId, UUID supplierId, UUID resolvedBy)
            implements DomainEvent {
        public static final String EVENT_TYPE = "dispute.resolved";

        @Override public String eventType() { return EVENT_TYPE; }
        @Override public int eventVersion() { return 1; }
        @Override public String aggregateId() { return String.valueOf(disputeId); }
    }

    public record DisputeClosedEvent(UUID eventId, Instant occurredAt,
                                      UUID disputeId, UUID orderId,
                                      UUID shopId, UUID supplierId, UUID closedBy)
            implements DomainEvent {
        public static final String EVENT_TYPE = "dispute.closed";

        @Override public String eventType() { return EVENT_TYPE; }
        @Override public int eventVersion() { return 1; }
        @Override public String aggregateId() { return String.valueOf(disputeId); }
    }
}
