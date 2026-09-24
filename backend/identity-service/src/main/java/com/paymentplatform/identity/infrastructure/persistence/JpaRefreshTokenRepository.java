package com.paymentplatform.identity.infrastructure.persistence;

import com.paymentplatform.identity.domain.model.RefreshToken;
import com.paymentplatform.identity.domain.repository.RefreshTokenRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

/** Adapter JPA du port RefreshTokenRepository. */
@Component
public class JpaRefreshTokenRepository implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpa;

    @PersistenceContext
    private EntityManager entityManager;

    public JpaRefreshTokenRepository(RefreshTokenJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity entity = toEntity(token);
        return toDomain(jpa.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return jpa.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    @Transactional
    public void revokeAllByUserId(UUID userId) {
        jpa.revokeAllByUserId(userId);
        // La requête bulk contourne le contexte de persistance : flush + clear
        // pour que les lectures suivantes voient la révocation dans la même transaction.
        entityManager.flush();
        entityManager.clear();
    }

    private RefreshTokenJpaEntity toEntity(RefreshToken token) {
        RefreshTokenJpaEntity e = new RefreshTokenJpaEntity();
        if (token.id() != null) {
            e.setId(token.id());
        }
        e.setTokenHash(token.tokenHash());
        e.setUserId(token.userId());
        e.setExpiresAt(token.expiresAt());
        e.setRevoked(token.revoked());
        e.setReplacedBy(token.replacedByHash());
        e.setCreatedAt(token.createdAt());
        return e;
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity e) {
        return RefreshToken.reconstruct(e.getId(), e.getTokenHash(), e.getUserId(),
                e.getExpiresAt(), e.isRevoked(), e.getReplacedBy(), e.getCreatedAt());
    }
}
