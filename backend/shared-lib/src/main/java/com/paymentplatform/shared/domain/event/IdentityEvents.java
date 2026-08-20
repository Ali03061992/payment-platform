package com.paymentplatform.shared.domain.event;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Événements du bounded context Identity. */
public final class IdentityEvents {

    private IdentityEvents() {
    }

    public record UserCreatedEvent(UUID eventId, Instant occurredAt, long userId, Long organizationId,
                                   List<String> roles) implements DomainEvent {
        public static final String EVENT_TYPE = "identity.user.created";

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
            return String.valueOf(userId);
        }
    }

    public record UserActivatedEvent(UUID eventId, Instant occurredAt, long userId, Long organizationId)
            implements DomainEvent {
        public static final String EVENT_TYPE = "identity.user.activated";

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
            return String.valueOf(userId);
        }
    }

    public record UserDisabledEvent(UUID eventId, Instant occurredAt, long userId, Long organizationId,
                                    String reason) implements DomainEvent {
        public static final String EVENT_TYPE = "identity.user.disabled";

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
            return String.valueOf(userId);
        }
    }
}