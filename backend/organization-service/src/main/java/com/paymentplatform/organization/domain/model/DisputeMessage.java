package com.paymentplatform.organization.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dispute_messages")
public class DisputeMessage {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "dispute_id", nullable = false, columnDefinition = "VARCHAR(36)", insertable = false, updatable = false)
    private UUID disputeId;

    @Column(name = "sender_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID senderId;

    @Column(name = "sender_role", nullable = false, length = 30)
    private String senderRole;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private Instant timestamp;

    @PrePersist
    void prePersist() {
        if (this.id == null) { this.id = UUID.randomUUID(); }
        if (timestamp == null) timestamp = Instant.now();
    }

    public static DisputeMessage create(UUID disputeId, UUID senderId, String senderRole, String content) {
        DisputeMessage msg = new DisputeMessage();
        msg.disputeId = disputeId;
        msg.senderId = senderId;
        msg.senderRole = senderRole;
        msg.content = content;
        msg.timestamp = Instant.now();
        return msg;
    }

    public UUID getId() { return id; }
    public UUID getDisputeId() { return disputeId; }
    public UUID getSenderId() { return senderId; }
    public String getSenderRole() { return senderRole; }
    public String getContent() { return content; }
    public Instant getTimestamp() { return timestamp; }
}
