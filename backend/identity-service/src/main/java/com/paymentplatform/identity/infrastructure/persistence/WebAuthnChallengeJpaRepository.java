package com.paymentplatform.identity.infrastructure.persistence;

import com.paymentplatform.identity.domain.model.WebAuthnChallengePurpose;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface WebAuthnChallengeJpaRepository extends JpaRepository<WebAuthnChallengeJpaEntity, UUID> {

    Optional<WebAuthnChallengeJpaEntity> findByChallengeAndPurposeAndConsumedFalse(
            byte[] challenge, WebAuthnChallengePurpose purpose);

    void deleteByExpiresAtBefore(Instant before);
}
