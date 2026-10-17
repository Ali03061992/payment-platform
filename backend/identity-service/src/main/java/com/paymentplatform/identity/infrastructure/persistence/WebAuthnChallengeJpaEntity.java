package com.paymentplatform.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import com.paymentplatform.identity.domain.model.WebAuthnChallengePurpose;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webauthn_challenges", indexes = {
        @Index(name = "idx_wachallenge_lookup", columnList = "challenge, purpose, consumed, expires_at"),
        @Index(name = "idx_wachallenge_expiry", columnList = "expires_at")
})
public class WebAuthnChallengeJpaEntity {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, columnDefinition = "VARBINARY(256)")
    private byte[] challenge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WebAuthnChallengePurpose purpose;

    @Column(name = "user_id", columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Column(name = "payload_json", nullable = false, columnDefinition = "TEXT")
    private String payloadJson;

    @Column(nullable = false)
    private boolean consumed;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected WebAuthnChallengeJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public byte[] getChallenge() {
        return challenge;
    }

    public void setChallenge(byte[] challenge) {
        this.challenge = challenge;
    }

    public WebAuthnChallengePurpose getPurpose() {
        return purpose;
    }

    public void setPurpose(WebAuthnChallengePurpose purpose) {
        this.purpose = purpose;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public boolean isConsumed() {
        return consumed;
    }

    public void setConsumed(boolean consumed) {
        this.consumed = consumed;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
