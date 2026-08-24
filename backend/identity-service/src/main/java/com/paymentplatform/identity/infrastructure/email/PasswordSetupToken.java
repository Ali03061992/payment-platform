package com.paymentplatform.identity.infrastructure.email;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_setup_tokens")
public class PasswordSetupToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false, unique = true, length = 64)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used")
    private boolean used = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected PasswordSetupToken() {}

    public PasswordSetupToken(Long userId, String token, Instant expiresAt) {
        this.userId = userId;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
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
}
