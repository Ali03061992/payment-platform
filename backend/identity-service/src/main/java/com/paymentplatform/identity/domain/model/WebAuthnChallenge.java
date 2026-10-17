package com.paymentplatform.identity.domain.model;

import com.paymentplatform.shared.domain.exception.DomainException;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

/**
 * Challenge WebAuthn éphémère à usage unique.
 * Le payload JSON des options émises est conservé pour rejouer la vérification
 * exacte (challenge, RP ID, origines) à la confirmation côté serveur.
 */
public class WebAuthnChallenge {

    private final UUID id;
    private final byte[] challenge;
    private final WebAuthnChallengePurpose purpose;
    private final UUID userId;
    private final String payloadJson;
    private boolean consumed;
    private final Instant expiresAt;
    private final Instant createdAt;

    private WebAuthnChallenge(UUID id, byte[] challenge, WebAuthnChallengePurpose purpose, UUID userId,
                              String payloadJson, boolean consumed, Instant expiresAt, Instant createdAt) {
        this.id = id;
        this.challenge = challenge;
        this.purpose = purpose;
        this.userId = userId;
        this.payloadJson = payloadJson;
        this.consumed = consumed;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
    }

    public static WebAuthnChallenge issue(UUID userId, WebAuthnChallengePurpose purpose, byte[] challenge,
                                          String payloadJson, Instant expiresAt) {
        if (challenge == null || challenge.length == 0) {
            throw new DomainException("Challenge WebAuthn manquant");
        }
        if (purpose == null) {
            throw new DomainException("Usage du challenge manquant");
        }
        if (payloadJson == null || payloadJson.isBlank()) {
            throw new DomainException("Payload du challenge manquant");
        }
        if (expiresAt == null || !expiresAt.isAfter(Instant.now())) {
            throw new DomainException("L'expiration du challenge doit être dans le futur");
        }
        return new WebAuthnChallenge(UUID.randomUUID(), challenge.clone(), purpose, userId,
                payloadJson, false, expiresAt, Instant.now());
    }

    public static WebAuthnChallenge reconstruct(UUID id, byte[] challenge, WebAuthnChallengePurpose purpose,
                                                UUID userId, String payloadJson, boolean consumed,
                                                Instant expiresAt, Instant createdAt) {
        return new WebAuthnChallenge(id, challenge, purpose, userId, payloadJson, consumed, expiresAt, createdAt);
    }

    /** Un challenge est valide s'il n'a pas été consommé, n'est pas expiré et correspond. */
    public boolean isValidFor(byte[] candidate) {
        return !consumed
                && expiresAt.isAfter(Instant.now())
                && candidate != null
                && MessageDigest.isEqual(challenge, candidate);
    }

    public void consume() {
        this.consumed = true;
    }

    public UUID id() {
        return id;
    }

    public byte[] challenge() {
        return challenge.clone();
    }

    public WebAuthnChallengePurpose purpose() {
        return purpose;
    }

    public UUID userId() {
        return userId;
    }

    public String payloadJson() {
        return payloadJson;
    }

    public boolean consumed() {
        return consumed;
    }

    public Instant expiresAt() {
        return expiresAt;
    }

    public Instant createdAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "WebAuthnChallenge{id=" + id + ", purpose=" + purpose + ", consumed=" + consumed
                + ", expiresAt=" + expiresAt + ", challenge=" + Arrays.toString(
                        Arrays.copyOf(challenge, Math.min(8, challenge.length))) + "...}";
    }
}
