package com.paymentplatform.organization.domain.model;

import java.util.UUID;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "order_events")
public class OrderEvent {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "order_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID orderId;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(name = "user_id", columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(columnDefinition = "TEXT")
    private String details;

    @PrePersist
    void prePersist() {
        if (this.id == null) { this.id = java.util.UUID.randomUUID(); }

        if (timestamp == null) timestamp = Instant.now();
    }

    public static OrderEvent create(UUID orderId, String action, UUID userId, String details) {
        OrderEvent event = new OrderEvent();
        event.orderId = orderId;
        event.action = action;
        event.userId = userId;
        event.timestamp = Instant.now();
        event.details = details;
        return event;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public String getAction() { return action; }
    public UUID getUserId() { return userId; }
    public Instant getTimestamp() { return timestamp; }
    public String getDetails() { return details; }
}
