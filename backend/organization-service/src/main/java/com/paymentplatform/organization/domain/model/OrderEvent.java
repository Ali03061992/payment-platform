package com.paymentplatform.organization.domain.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "order_events")
public class OrderEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(name = "user_id")
    private Long userId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(columnDefinition = "TEXT")
    private String details;

    @PrePersist
    void prePersist() {
        if (timestamp == null) timestamp = Instant.now();
    }

    public static OrderEvent create(Long orderId, String action, Long userId, String details) {
        OrderEvent event = new OrderEvent();
        event.orderId = orderId;
        event.action = action;
        event.userId = userId;
        event.timestamp = Instant.now();
        event.details = details;
        return event;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getAction() { return action; }
    public Long getUserId() { return userId; }
    public Instant getTimestamp() { return timestamp; }
    public String getDetails() { return details; }
}
