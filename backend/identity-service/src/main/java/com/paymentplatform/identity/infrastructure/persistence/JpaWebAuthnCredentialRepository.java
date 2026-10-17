package com.paymentplatform.identity.infrastructure.persistence;

import com.paymentplatform.identity.domain.model.WebAuthnCredentialSummary;
import com.paymentplatform.identity.domain.repository.WebAuthnCredentialRepository;
import com.paymentplatform.identity.infrastructure.webauthn.WebAuthnSupport;
import org.springframework.security.web.webauthn.api.AuthenticatorTransport;
import org.springframework.security.web.webauthn.api.Bytes;
import org.springframework.security.web.webauthn.api.CredentialRecord;
import org.springframework.security.web.webauthn.api.ImmutableCredentialRecord;
import org.springframework.security.web.webauthn.management.UserCredentialRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Adapter JPA unique : port de gestion (identity) + repository WebAuthn de
 * Spring Security ({@link UserCredentialRepository}). Seuls les credentials
 * ACTIVE sont exposés à la vérification d'assertion — une passkey révoquée
 * ne peut plus authentifier.
 */
@Component
public class JpaWebAuthnCredentialRepository
        implements WebAuthnCredentialRepository, UserCredentialRepository {

    private final WebAuthnCredentialJpaRepository jpa;

    public JpaWebAuthnCredentialRepository(WebAuthnCredentialJpaRepository jpa) {
        this.jpa = jpa;
    }

    // ---------------------------------------------------------------------
    // UserCredentialRepository (Spring Security / webauthn4j)
    // ---------------------------------------------------------------------

    @Override
    @Transactional
    public void save(CredentialRecord record) {
        byte[] credentialId = record.getCredentialId().getBytes();
        WebAuthnCredentialJpaEntity entity = jpa.findByCredentialId(credentialId)
                .orElseGet(WebAuthnCredentialJpaEntity::new);
        if (entity.getId() == null) {
            entity.setId(UUID.randomUUID());
        }
        entity.setUserId(WebAuthnSupport.userIdFromHandle(record.getUserEntityUserId()));
        entity.setCredentialId(credentialId);
        entity.setPublicKey(record.getPublicKey().getBytes());
        entity.setAttestationObject(record.getAttestationObject() == null
                ? entity.getAttestationObject()
                : record.getAttestationObject().getBytes());
        entity.setSignatureCount(record.getSignatureCount());
        entity.setLabel(record.getLabel() == null || record.getLabel().isBlank()
                ? "Passkey" : record.getLabel());
        entity.setCredentialType(record.getCredentialType().getValue());
        entity.setTransports(record.getTransports() == null || record.getTransports().isEmpty()
                ? null
                : String.join(",", record.getTransports().stream().map(AuthenticatorTransport::getValue).toList()));
        entity.setUvInitialized(record.isUvInitialized());
        entity.setBackupEligible(record.isBackupEligible());
        entity.setBackupState(record.isBackupState());
        entity.setStatus(WebAuthnCredentialJpaEntity.STATUS_ACTIVE);
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(Instant.now());
        }
        entity.setLastUsedAt(record.getLastUsed() == null ? entity.getLastUsedAt() : record.getLastUsed());
        jpa.save(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public CredentialRecord findByCredentialId(Bytes credentialId) {
        return jpa.findByCredentialIdAndStatus(credentialId.getBytes(), WebAuthnCredentialJpaEntity.STATUS_ACTIVE)
                .map(this::toCredentialRecord)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CredentialRecord> findByUserId(Bytes userHandle) {
        UUID userId = WebAuthnSupport.userIdFromHandle(userHandle);
        return jpa.findByUserIdAndStatusOrderByCreatedAtDesc(userId, WebAuthnCredentialJpaEntity.STATUS_ACTIVE)
                .stream()
                .map(this::toCredentialRecord)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Bytes credentialId) {
        jpa.findByCredentialId(credentialId.getBytes()).ifPresent(jpa::delete);
    }

    // ---------------------------------------------------------------------
    // Port de gestion (identity)
    // ---------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<WebAuthnCredentialSummary> findByUserId(UUID userId) {
        return jpa.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toSummary).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countActiveByUserId(UUID userId) {
        return jpa.countByUserIdAndStatus(userId, WebAuthnCredentialJpaEntity.STATUS_ACTIVE);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WebAuthnCredentialSummary> findActiveByIdForUser(UUID id, UUID userId) {
        return jpa.findByIdAndUserIdAndStatus(id, userId, WebAuthnCredentialJpaEntity.STATUS_ACTIVE)
                .map(this::toSummary);
    }

    @Override
    @Transactional
    public boolean rename(UUID id, UUID userId, String label) {
        Optional<WebAuthnCredentialJpaEntity> existing =
                jpa.findByIdAndUserIdAndStatus(id, userId, WebAuthnCredentialJpaEntity.STATUS_ACTIVE);
        if (existing.isEmpty()) {
            return false;
        }
        WebAuthnCredentialJpaEntity entity = existing.get();
        entity.setLabel(label);
        jpa.save(entity);
        return true;
    }

    @Override
    @Transactional
    public boolean revoke(UUID id, UUID userId) {
        Optional<WebAuthnCredentialJpaEntity> existing =
                jpa.findByIdAndUserIdAndStatus(id, userId, WebAuthnCredentialJpaEntity.STATUS_ACTIVE);
        if (existing.isEmpty()) {
            return false;
        }
        WebAuthnCredentialJpaEntity entity = existing.get();
        entity.setStatus(WebAuthnCredentialJpaEntity.STATUS_REVOKED);
        jpa.save(entity);
        return true;
    }

    @Override
    @Transactional
    public void touchByCredentialId(byte[] credentialId, long signatureCount) {
        jpa.findByCredentialId(credentialId).ifPresent(entity -> {
            entity.setSignatureCount(Math.max(entity.getSignatureCount(), signatureCount));
            entity.setLastUsedAt(Instant.now());
            jpa.save(entity);
        });
    }

    // ---------------------------------------------------------------------
    // Mapping
    // ---------------------------------------------------------------------

    private CredentialRecord toCredentialRecord(WebAuthnCredentialJpaEntity e) {
        return ImmutableCredentialRecord.builder()
                .credentialType(org.springframework.security.web.webauthn.api.PublicKeyCredentialType.PUBLIC_KEY)
                .credentialId(new Bytes(e.getCredentialId()))
                .publicKey(new org.springframework.security.web.webauthn.api.ImmutablePublicKeyCose(e.getPublicKey()))
                .attestationObject(e.getAttestationObject() == null ? null : new Bytes(e.getAttestationObject()))
                .signatureCount(e.getSignatureCount())
                .uvInitialized(e.isUvInitialized())
                .transports(parseTransports(e.getTransports()))
                .backupEligible(e.isBackupEligible())
                .backupState(e.isBackupState())
                .userEntityUserId(WebAuthnSupport.userHandle(e.getUserId()))
                .label(e.getLabel())
                .created(e.getCreatedAt())
                .lastUsed(e.getLastUsedAt())
                .build();
    }

    private java.util.Set<AuthenticatorTransport> parseTransports(String transports) {
        if (transports == null || transports.isBlank()) {
            return java.util.Set.of();
        }
        return Arrays.stream(transports.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(this::toTransport)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private AuthenticatorTransport toTransport(String value) {
        for (AuthenticatorTransport transport : AuthenticatorTransport.values()) {
            if (transport.getValue().equalsIgnoreCase(value)) {
                return transport;
            }
        }
        return AuthenticatorTransport.INTERNAL;
    }

    private WebAuthnCredentialSummary toSummary(WebAuthnCredentialJpaEntity e) {
        return new WebAuthnCredentialSummary(
                e.getId(),
                e.getUserId(),
                new Bytes(e.getCredentialId()).toBase64UrlString(),
                e.getLabel(),
                e.getTransports() == null || e.getTransports().isBlank()
                        ? List.of()
                        : Arrays.stream(e.getTransports().split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList(),
                WebAuthnCredentialJpaEntity.STATUS_REVOKED.equals(e.getStatus()),
                e.getCreatedAt(),
                e.getLastUsedAt());
    }
}
