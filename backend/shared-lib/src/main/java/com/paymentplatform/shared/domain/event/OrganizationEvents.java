package com.paymentplatform.shared.domain.event;

import java.time.Instant;
import java.util.UUID;

/** Événements du bounded context Organization. */
public final class OrganizationEvents {

    private OrganizationEvents() {
    }

    public record SupplierCreatedEvent(UUID eventId, Instant occurredAt, UUID organizationId, String name)
            implements DomainEvent {
        public static final String EVENT_TYPE = "organization.supplier.created";

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
            return String.valueOf(organizationId);
        }
    }

    public record SupplierActivatedEvent(UUID eventId, Instant occurredAt, UUID organizationId) implements DomainEvent {
        public static final String EVENT_TYPE = "organization.supplier.activated";

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
            return String.valueOf(organizationId);
        }
    }

    public record SupplierDisabledEvent(UUID eventId, Instant occurredAt, UUID organizationId) implements DomainEvent {
        public static final String EVENT_TYPE = "organization.supplier.disabled";

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
            return String.valueOf(organizationId);
        }
    }

    public record ShopCreatedEvent(UUID eventId, Instant occurredAt, UUID organizationId, String name)
            implements DomainEvent {
        public static final String EVENT_TYPE = "organization.shop.created";

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
            return String.valueOf(organizationId);
        }
    }

    public record ShopActivatedEvent(UUID eventId, Instant occurredAt, UUID organizationId) implements DomainEvent {
        public static final String EVENT_TYPE = "organization.shop.activated";

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
            return String.valueOf(organizationId);
        }
    }

    public record ShopDisabledEvent(UUID eventId, Instant occurredAt, UUID organizationId) implements DomainEvent {
        public static final String EVENT_TYPE = "organization.shop.disabled";

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
            return String.valueOf(organizationId);
        }
    }
}