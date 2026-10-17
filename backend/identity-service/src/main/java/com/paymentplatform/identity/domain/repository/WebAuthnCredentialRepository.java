package com.paymentplatform.identity.domain.repository;

import com.paymentplatform.identity.domain.model.WebAuthnCredentialSummary;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Port de gestion des passkeys d'un utilisateur (appareils enregistrés). */
public interface WebAuthnCredentialRepository {

    List<WebAuthnCredentialSummary> findByUserId(UUID userId);

    long countActiveByUserId(UUID userId);

    Optional<WebAuthnCredentialSummary> findActiveByIdForUser(UUID id, UUID userId);

    /** Renommage (ownership vérifié). Retourne false si absent ou non possédé. */
    boolean rename(UUID id, UUID userId, String label);

    /** Révocation logique (ownership vérifié). Retourne false si absent ou non possédé. */
    boolean revoke(UUID id, UUID userId);

    /** Mise à jour du compteur de signature et de la dernière utilisation. */
    void touchByCredentialId(byte[] credentialId, long signatureCount);
}
