package com.paymentplatform.identity.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WebAuthnCredentialJpaRepository extends JpaRepository<WebAuthnCredentialJpaEntity, UUID> {

    Optional<WebAuthnCredentialJpaEntity> findByCredentialId(byte[] credentialId);

    Optional<WebAuthnCredentialJpaEntity> findByCredentialIdAndStatus(byte[] credentialId, String status);

    List<WebAuthnCredentialJpaEntity> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<WebAuthnCredentialJpaEntity> findByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);

    Optional<WebAuthnCredentialJpaEntity> findByIdAndUserIdAndStatus(UUID id, UUID userId, String status);

    long countByUserIdAndStatus(UUID userId, String status);
}
