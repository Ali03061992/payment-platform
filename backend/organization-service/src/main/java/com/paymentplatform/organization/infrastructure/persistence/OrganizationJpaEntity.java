package com.paymentplatform.organization.infrastructure.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "organizations")
public class OrganizationJpaEntity {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false, length = 20)
    private String status;

    /** Matricule fiscal (carte d'identification fiscale), mention art. 18 Code TVA. */
    @Column(name = "matricule_fiscal", length = 32)
    private String matriculeFiscal;

    @Column(length = 32)
    private String patente;

    @Column(name = "tej_identifier", length = 64)
    private String tejIdentifier;

    @Column(name = "default_withholding_nature", length = 30)
    private String defaultWithholdingNature;

    @Column(length = 255)
    private String address;

    @Column(name = "fiscal_regime", nullable = false, length = 30)
    private String fiscalRegime = "ASSUJETTI";

    @Column(name = "suspension_attestation_number", length = 64)
    private String suspensionAttestationNumber;

    @Column(name = "suspension_attestation_date")
    private java.time.LocalDate suspensionAttestationDate;

    @Column(name = "suspension_expiry_date")
    private java.time.LocalDate suspensionExpiryDate;

    @Column(name = "teif_enabled", nullable = false)
    private boolean teifEnabled = true;

    @Column(name = "tej_enabled", nullable = false)
    private boolean tejEnabled = true;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OrganizationJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getMatriculeFiscal() { return matriculeFiscal; }
    public void setMatriculeFiscal(String matriculeFiscal) { this.matriculeFiscal = matriculeFiscal; }
    public String getPatente() { return patente; }
    public void setPatente(String patente) { this.patente = patente; }
    public String getTejIdentifier() { return tejIdentifier; }
    public void setTejIdentifier(String tejIdentifier) { this.tejIdentifier = tejIdentifier; }
    public String getDefaultWithholdingNature() { return defaultWithholdingNature; }
    public void setDefaultWithholdingNature(String v) { this.defaultWithholdingNature = v; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getFiscalRegime() { return fiscalRegime; }
    public void setFiscalRegime(String fiscalRegime) { this.fiscalRegime = fiscalRegime; }
    public String getSuspensionAttestationNumber() { return suspensionAttestationNumber; }
    public void setSuspensionAttestationNumber(String v) { this.suspensionAttestationNumber = v; }
    public java.time.LocalDate getSuspensionAttestationDate() { return suspensionAttestationDate; }
    public void setSuspensionAttestationDate(java.time.LocalDate v) { this.suspensionAttestationDate = v; }
    public java.time.LocalDate getSuspensionExpiryDate() { return suspensionExpiryDate; }
    public void setSuspensionExpiryDate(java.time.LocalDate v) { this.suspensionExpiryDate = v; }
    public boolean isTeifEnabled() { return teifEnabled; }
    public void setTeifEnabled(boolean teifEnabled) { this.teifEnabled = teifEnabled; }
    public boolean isTejEnabled() { return tejEnabled; }
    public void setTejEnabled(boolean tejEnabled) { this.tejEnabled = tejEnabled; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) { this.id = java.util.UUID.randomUUID(); }
    }
}
