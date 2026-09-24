package com.paymentplatform.identity.domain.model;

import com.paymentplatform.shared.domain.exception.DomainException;

import java.time.Instant;
import java.util.UUID;

/**
 * M1 : refresh token opaque persistant (rotation + révocation).
 * Seule l'empreinte SHA-256 du token est stockée ; le brut n'est transmis
 * qu'une seule fois, à l'émission.
 */
public class RefreshToken {

    private final UUID id;
    private final String tokenHash;
    private final UUID userId;
    private final Instant expiresAt;
    private boolean revoked;
    private String replacedByHash;
    private final Instant createdAt;

    private RefreshToken(UUID id, String tokenHash, UUID userId, Instant expiresAt,
                         boolean revoked, String replacedByHash, Instant createdAt) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.userId = userId;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.replacedByHash = replacedByHash;
        this.createdAt = createdAt;
    }

    public static RefreshToken issue(UUID userId, String tokenHash, Instant expiresAt) {
        if (userId == null) throw new DomainException("L'utilisateur est obligatoire");
        if (tokenHash == null || tokenHash.isBlank()) throw new DomainException("L'empreinte est obligatoire");
        if (expiresAt == null || !expiresAt.isAfter(Instant.now()))
            throw new DomainException("L'expiration doit être dans le futur");
        Instant now = Instant.now();
        return new RefreshToken(null, tokenHash, userId, expiresAt, false, null, now);
    }

    public static RefreshToken reconstruct(UUID id, String tokenHash, UUID userId, Instant expiresAt,
                                           boolean revoked, String replacedByHash, Instant createdAt) {
        return new RefreshToken(id, tokenHash, userId, expiresAt, revoked, replacedByHash, createdAt);
    }

    public boolean isExpired() {
        return !expiresAt.isAfter(Instant.now());
    }

    public boolean isActive() {
        return !revoked && !isExpired();
    }

    public void revoke() {
        this.revoked = true;
    }

    public void markReplaced(String newTokenHash) {
        this.replacedByHash = newTokenHash;
        this.revoked = true;
    }

    public UUID id() { return id; }
    public String tokenHash() { return tokenHash; }
    public UUID userId() { return userId; }
    public Instant expiresAt() { return expiresAt; }
    public boolean revoked() { return revoked; }
    public String replacedByHash() { return replacedByHash; }
    public Instant createdAt() { return createdAt; }
}
