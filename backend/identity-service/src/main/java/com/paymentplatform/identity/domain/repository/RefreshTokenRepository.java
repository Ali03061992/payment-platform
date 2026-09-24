package com.paymentplatform.identity.domain.repository;

import com.paymentplatform.identity.domain.model.RefreshToken;

import java.util.Optional;
import java.util.UUID;

/** Port de persistance des refresh tokens (implémenté en infrastructure). */
public interface RefreshTokenRepository {

    RefreshToken save(RefreshToken token);

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    void revokeAllByUserId(UUID userId);
}
