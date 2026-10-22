package com.paymentplatform.identity.infrastructure.persistence;

import com.paymentplatform.identity.domain.model.WebAuthnChallengePurpose;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface WebAuthnChallengeJpaRepository extends JpaRepository<WebAuthnChallengeJpaEntity, UUID> {

    Optional<WebAuthnChallengeJpaEntity> findByChallengeAndPurposeAndConsumedFalse(
            byte[] challenge, WebAuthnChallengePurpose purpose);

    @Modifying
    @Query("""
            update WebAuthnChallengeJpaEntity c
               set c.consumed = true
             where c.id = :id
               and c.consumed = false
               and c.expiresAt > :now
            """)
    int consumeIfValid(@Param("id") UUID id, @Param("now") Instant now);

    void deleteByExpiresAtBefore(Instant before);
}
