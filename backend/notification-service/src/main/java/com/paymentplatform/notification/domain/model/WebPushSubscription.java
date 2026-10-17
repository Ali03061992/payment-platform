package com.paymentplatform.notification.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Abonnement Web Push (PWA) : endpoint + clés d'authentification chiffrées. */
@Entity
@Table(name = "web_push_subscriptions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"endpoint"})
})
public class WebPushSubscription {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Column(name = "organization_id", columnDefinition = "VARCHAR(36)")
    private UUID organizationId;

    @Column(nullable = false, length = 768)
    private String endpoint;

    @Column(name = "p256dh", nullable = false)
    private String p256dh;

    @Column(name = "auth", nullable = false)
    private String auth;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    protected WebPushSubscription() {}

    public WebPushSubscription(UUID userId, UUID organizationId, String endpoint,
                               String p256dh, String auth, String userAgent) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.organizationId = organizationId;
        this.endpoint = endpoint;
        this.p256dh = p256dh;
        this.auth = auth;
        this.userAgent = userAgent != null && userAgent.length() > 512
                ? userAgent.substring(0, 512) : userAgent;
        this.createdAt = Instant.now();
        this.lastUsedAt = Instant.now();
    }

    public void touch() {
        this.lastUsedAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID organizationId() { return organizationId; }
    public String endpoint() { return endpoint; }
    public String p256dh() { return p256dh; }
    public String auth() { return auth; }
    public String userAgent() { return userAgent; }
    public Instant createdAt() { return createdAt; }
    public Instant lastUsedAt() { return lastUsedAt; }
}
