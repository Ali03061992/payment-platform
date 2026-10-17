package com.paymentplatform.identity.infrastructure.persistence;

import com.paymentplatform.identity.domain.model.WebAuthnChallenge;
import com.paymentplatform.identity.domain.model.WebAuthnChallengePurpose;
import com.paymentplatform.identity.domain.repository.WebAuthnChallengeRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/** Adapter JPA du port WebAuthnChallengeRepository. */
@Component
public class JpaWebAuthnChallengeRepository implements WebAuthnChallengeRepository {

    private final WebAuthnChallengeJpaRepository jpa;

    public JpaWebAuthnChallengeRepository(WebAuthnChallengeJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public WebAuthnChallenge save(WebAuthnChallenge challenge) {
        WebAuthnChallengeJpaEntity entity = new WebAuthnChallengeJpaEntity();
        entity.setId(challenge.id());
        entity.setChallenge(challenge.challenge());
        entity.setPurpose(challenge.purpose());
        entity.setUserId(challenge.userId());
        entity.setPayloadJson(challenge.payloadJson());
        entity.setConsumed(challenge.consumed());
        entity.setExpiresAt(challenge.expiresAt());
        entity.setCreatedAt(challenge.createdAt());
        WebAuthnChallengeJpaEntity saved = jpa.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WebAuthnChallenge> findValid(byte[] challenge, WebAuthnChallengePurpose purpose) {
        return jpa.findByChallengeAndPurposeAndConsumedFalse(challenge, purpose)
                .filter(e -> e.getExpiresAt().isAfter(Instant.now()))
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public void deleteExpired(Instant before) {
        jpa.deleteByExpiresAtBefore(before);
    }

    private WebAuthnChallenge toDomain(WebAuthnChallengeJpaEntity e) {
        return WebAuthnChallenge.reconstruct(e.getId(), e.getChallenge(), e.getPurpose(), e.getUserId(),
                e.getPayloadJson(), e.isConsumed(), e.getExpiresAt(), e.getCreatedAt());
    }
}
