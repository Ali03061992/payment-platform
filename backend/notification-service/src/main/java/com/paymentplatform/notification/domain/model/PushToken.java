package com.paymentplatform.notification.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "push_tokens", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "token"})
})
public class PushToken {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Column(name = "organization_id", columnDefinition = "VARCHAR(36)")
    private UUID organizationId;

    @Column(nullable = false)
    private String token;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    protected PushToken() {}

    public PushToken(UUID userId, UUID organizationId, String token) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.organizationId = organizationId;
        this.token = token;
        this.createdAt = Instant.now();
        this.lastUsedAt = Instant.now();
    }

    public void touch() {
        this.lastUsedAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public UUID organizationId() { return organizationId; }
    public String token() { return token; }
    public Instant createdAt() { return createdAt; }
    public Instant lastUsedAt() { return lastUsedAt; }
}
