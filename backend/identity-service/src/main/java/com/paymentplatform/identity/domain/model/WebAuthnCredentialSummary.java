package com.paymentplatform.identity.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Vue de gestion d'une passkey enregistrée (appareil).
 * N'expose jamais la clé publique ni les données d'attestation.
 */
public record WebAuthnCredentialSummary(UUID id, UUID userId, String credentialIdBase64Url, String label,
                                        List<String> transports, boolean revoked,
                                        Instant createdAt, Instant lastUsedAt) {
}
