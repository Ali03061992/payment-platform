package com.paymentplatform.payment.infrastructure.persistence;

import java.util.UUID;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "payment_events")
public class PaymentEventJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "payment_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID paymentId;

    @Column(nullable = false, length = 40)
    private String action;

    @Column(name = "user_id", columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Column(nullable = false)
    private Instant timestamp;

    @Column(columnDefinition = "TEXT")
    private String details;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPaymentId() { return paymentId; }
    public void setPaymentId(UUID paymentId) { this.paymentId = paymentId; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
    public String getDetails() { return details; }
    public void setDetails(String details) { this.details = details; }
}
