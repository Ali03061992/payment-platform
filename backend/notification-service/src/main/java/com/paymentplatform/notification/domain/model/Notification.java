package com.paymentplatform.notification.domain.model;

import java.util.UUID;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "recipient_user_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID recipientUserId;

    @Column(name = "recipient_organization_id", columnDefinition = "VARCHAR(36)")
    private UUID recipientOrganizationId;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "read_status", nullable = false, length = 20)
    private String readStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "related_entity_id", length = 64)
    private String relatedEntityId;

    @Column(name = "related_entity_type", length = 40)
    private String relatedEntityType;

    protected Notification() {}

    public Notification(UUID recipientUserId, UUID recipientOrganizationId,
                        String type, String message, String relatedEntityType, String relatedEntityId) {
        this.recipientUserId = recipientUserId;
        this.recipientOrganizationId = recipientOrganizationId;
        this.type = type;
        this.message = message;
        this.readStatus = "UNREAD";
        this.createdAt = Instant.now();
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
    }

    public void markAsRead() {
        this.readStatus = "READ";
        this.readAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID recipientUserId() { return recipientUserId; }
    public UUID recipientOrganizationId() { return recipientOrganizationId; }
    public String type() { return type; }
    public String message() { return message; }
    public String readStatus() { return readStatus; }
    public Instant createdAt() { return createdAt; }
    public Instant readAt() { return readAt; }
    public String relatedEntityType() { return relatedEntityType; }
    public String relatedEntityId() { return relatedEntityId; }
}
