package com.paymentplatform.identity.infrastructure.email;

import java.util.UUID;
import jakarta.persistence.PrePersist;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_setup_tokens")
public class PasswordSetupToken {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used")
    private boolean used = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected PasswordSetupToken() {}

    public PasswordSetupToken(UUID userId, String token, Instant expiresAt) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getToken() { return token; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }
    public Instant getCreatedAt() { return createdAt; }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public void markUsed() {
        this.used = true;
    }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) { this.id = java.util.UUID.randomUUID(); }
    }
}
