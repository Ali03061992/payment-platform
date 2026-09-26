package com.paymentplatform.shared.infrastructure.outbox;

import com.paymentplatform.shared.domain.event.DomainEvent;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Ligne d'outbox écrite dans la même transaction que l'agrégat (Outbox Pattern). */
@Entity
@Table(name = "outbox_events")
public class OutboxEventEntity {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "aggregate_id", nullable = false, length = 64)
    private String aggregateId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    protected OutboxEventEntity() {
    }

    public OutboxEventEntity(DomainEvent event, String payload) {
        this.eventId = event.eventId().toString();
        this.eventType = event.eventType();
        this.aggregateId = event.aggregateId();
        this.payload = payload;
        this.createdAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }

    public void markProcessed() {
        this.processedAt = Instant.now();
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) { this.id = java.util.UUID.randomUUID(); }
    }
}