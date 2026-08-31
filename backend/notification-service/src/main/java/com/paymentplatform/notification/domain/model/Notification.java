package com.paymentplatform.notification.domain.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_user_id", nullable = false)
    private Long recipientUserId;

    @Column(name = "recipient_organization_id")
    private Long recipientOrganizationId;

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

    public Notification(Long recipientUserId, Long recipientOrganizationId,
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

    public Long id() { return id; }
    public Long recipientUserId() { return recipientUserId; }
    public Long recipientOrganizationId() { return recipientOrganizationId; }
    public String type() { return type; }
    public String message() { return message; }
    public String readStatus() { return readStatus; }
    public Instant createdAt() { return createdAt; }
    public Instant readAt() { return readAt; }
    public String relatedEntityType() { return relatedEntityType; }
    public String relatedEntityId() { return relatedEntityId; }
}
