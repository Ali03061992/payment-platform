package com.paymentplatform.notification.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fcm_tokens", indexes = {
    @Index(name = "idx_fcm_user", columnList = "user_id"),
    @Index(name = "idx_fcm_token", columnList = "token", unique = true)
})
public class FcmToken {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Column(nullable = false, length = 512)
    private String token;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at", nullable = false)
    private Instant lastUsedAt;

    protected FcmToken() {}

    public FcmToken(UUID userId, String token) {
        this.id = UUID.randomUUID();
        this.userId = userId;
        this.token = token;
        this.createdAt = Instant.now();
        this.lastUsedAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID userId() { return userId; }
    public String token() { return token; }
    public Instant createdAt() { return createdAt; }
    public Instant lastUsedAt() { return lastUsedAt; }

    public void touch() {
        this.lastUsedAt = Instant.now();
    }
}
