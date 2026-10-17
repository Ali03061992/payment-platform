package com.paymentplatform.identity.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webauthn_credentials", indexes = {
        @Index(name = "idx_wac_credential_id", columnList = "credential_id", unique = true),
        @Index(name = "idx_wac_user_status", columnList = "user_id, status")
})
public class WebAuthnCredentialJpaEntity {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_REVOKED = "REVOKED";

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Column(name = "credential_id", nullable = false, columnDefinition = "VARBINARY(1023)")
    private byte[] credentialId;

    @Column(name = "public_key", nullable = false, columnDefinition = "VARBINARY(1024)")
    private byte[] publicKey;

    /** Attestation "none" initiale — requise par webauthn4j à l'assertion. */
    @Column(name = "attestation_object", columnDefinition = "VARBINARY(4096)")
    private byte[] attestationObject;

    @Column(name = "signature_count", nullable = false)
    private long signatureCount;

    @Column(nullable = false, length = 100)
    private String label;

    @Column(name = "credential_type", nullable = false, length = 32)
    private String credentialType;

    @Column(length = 120)
    private String transports;

    @Column(name = "uv_initialized", nullable = false)
    private boolean uvInitialized;

    @Column(name = "backup_eligible", nullable = false)
    private boolean backupEligible;

    @Column(name = "backup_state", nullable = false)
    private boolean backupState;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    protected WebAuthnCredentialJpaEntity() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public byte[] getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(byte[] credentialId) {
        this.credentialId = credentialId;
    }

    public byte[] getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(byte[] publicKey) {
        this.publicKey = publicKey;
    }

    public byte[] getAttestationObject() {
        return attestationObject;
    }

    public void setAttestationObject(byte[] attestationObject) {
        this.attestationObject = attestationObject;
    }

    public long getSignatureCount() {
        return signatureCount;
    }

    public void setSignatureCount(long signatureCount) {
        this.signatureCount = signatureCount;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getCredentialType() {
        return credentialType;
    }

    public void setCredentialType(String credentialType) {
        this.credentialType = credentialType;
    }

    public String getTransports() {
        return transports;
    }

    public void setTransports(String transports) {
        this.transports = transports;
    }

    public boolean isUvInitialized() {
        return uvInitialized;
    }

    public void setUvInitialized(boolean uvInitialized) {
        this.uvInitialized = uvInitialized;
    }

    public boolean isBackupEligible() {
        return backupEligible;
    }

    public void setBackupEligible(boolean backupEligible) {
        this.backupEligible = backupEligible;
    }

    public boolean isBackupState() {
        return backupState;
    }

    public void setBackupState(boolean backupState) {
        this.backupState = backupState;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
