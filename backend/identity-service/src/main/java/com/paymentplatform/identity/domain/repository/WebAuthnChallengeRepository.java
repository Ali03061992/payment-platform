package com.paymentplatform.identity.domain.repository;

import com.paymentplatform.identity.domain.model.WebAuthnChallenge;
import com.paymentplatform.identity.domain.model.WebAuthnChallengePurpose;

import java.time.Instant;
import java.util.Optional;

/** Port de persistance des challenges WebAuthn (implémenté en infrastructure). */
public interface WebAuthnChallengeRepository {

    WebAuthnChallenge save(WebAuthnChallenge challenge);

    /** Recherche d'un challenge valide (non consommé, non expiré) pour un usage donné. */
    Optional<WebAuthnChallenge> findValid(byte[] challenge, WebAuthnChallengePurpose purpose);

    /** Consomme atomiquement un challenge valide ; false si déjà consommé ou expiré. */
    boolean consumeIfValid(java.util.UUID id, Instant now);

    /** Purge des challenges expirés (nettoyage périodique). */
    void deleteExpired(Instant before);
}
