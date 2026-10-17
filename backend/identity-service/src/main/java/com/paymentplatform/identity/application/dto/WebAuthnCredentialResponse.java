package com.paymentplatform.identity.application.dto;

import com.paymentplatform.identity.domain.model.WebAuthnCredentialSummary;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Appareil (passkey) exposé à l'utilisateur — jamais la clé publique. */
public record WebAuthnCredentialResponse(UUID id, String label, String credentialId, String status,
                                         List<String> transports, Instant createdAt, Instant lastUsedAt) {

    public static WebAuthnCredentialResponse from(WebAuthnCredentialSummary summary) {
        return new WebAuthnCredentialResponse(summary.id(), summary.label(), summary.credentialIdBase64Url(),
                summary.revoked() ? "REVOKED" : "ACTIVE", summary.transports(),
                summary.createdAt(), summary.lastUsedAt());
    }
}
