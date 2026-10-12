package com.paymentplatform.organization.domain.model;

import com.paymentplatform.organization.domain.valueobject.MatriculeFiscal;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.domain.valueobject.OrganizationName;
import com.paymentplatform.organization.domain.valueobject.OrganizationStatus;
import com.paymentplatform.organization.domain.valueobject.OrganizationType;
import com.paymentplatform.shared.domain.exception.DomainException;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Aggregate Root Organization.
 * Invariants : nom unique, type SUPPLIER|SHOP, statut ACTIVE|DISABLED.
 * La désactivation est idempotente, la réactivation est idempotente.
 */
public class Organization {

    private final OrganizationId id;
    private OrganizationName name;
    private final OrganizationType type;
    private OrganizationStatus status;
    private long version;
    private final Instant createdAt;
    private Instant updatedAt;

    // Profil fiscal tunisien (mentions obligatoires art. 18 Code TVA).
    private String matriculeFiscal;
    private String patente;
    /** Identifiant du compte TEJ (optionnel, pour le dépôt des certificats RAS). */
    private String tejIdentifier;
    /**
     * Nature de retenue par défaut de ce fournisseur (ex. HONORAIRES_REEL pour un
     * prestataire au réel) : prime sur le défaut MARCHANDISES lors de l'émission,
     * sauf nature explicite et sauf SANS_MF (15% d'office sans MF).
     */
    private String defaultWithholdingNature;
    private String address;
    private FiscalRegime fiscalRegime = FiscalRegime.ASSUJETTI;
    private String suspensionAttestationNumber;
    private LocalDate suspensionAttestationDate;
    private LocalDate suspensionExpiryDate;

    private Organization(OrganizationId id, OrganizationName name, OrganizationType type,
                         OrganizationStatus status, long version, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.status = status;
        this.version = version;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Organization create(OrganizationId id, OrganizationName name, OrganizationType type) {
        if (name == null) {
            throw new DomainException("Le nom est requis");
        }
        if (type == null) {
            throw new DomainException("Le type est requis");
        }
        Instant now = Instant.now();
        return new Organization(id, name, type, OrganizationStatus.ACTIVE, 0, now, now);
    }

    public static Organization reconstruct(OrganizationId id, OrganizationName name, OrganizationType type,
                                           OrganizationStatus status, long version,
                                           Instant createdAt, Instant updatedAt) {
        return new Organization(id, name, type, status, version, createdAt, updatedAt);
    }

    public static Organization reconstruct(OrganizationId id, OrganizationName name, OrganizationType type,
                                           OrganizationStatus status, long version,
                                           Instant createdAt, Instant updatedAt,
                                           String matriculeFiscal, String patente, String address,
                                           FiscalRegime fiscalRegime,
                                           String attestationNumber, LocalDate attestationDate,
                                           LocalDate expiryDate, String tejIdentifier) {
        Organization org = new Organization(id, name, type, status, version, createdAt, updatedAt);
        org.matriculeFiscal = matriculeFiscal;
        org.patente = patente;
        org.tejIdentifier = tejIdentifier;
        org.address = address;
        org.fiscalRegime = fiscalRegime != null ? fiscalRegime : FiscalRegime.ASSUJETTI;
        org.suspensionAttestationNumber = attestationNumber;
        org.suspensionAttestationDate = attestationDate;
        org.suspensionExpiryDate = expiryDate;
        return org;
    }

    public void disable() {
        if (status == OrganizationStatus.DISABLED) {
            return;
        }
        this.status = OrganizationStatus.DISABLED;
        this.updatedAt = Instant.now();
    }

    public void activate() {
        if (status == OrganizationStatus.ACTIVE) {
            return;
        }
        this.status = OrganizationStatus.ACTIVE;
        this.updatedAt = Instant.now();
    }

    public void updateName(OrganizationName newName) {
        if (newName == null) {
            throw new DomainException("Le nom est requis");
        }
        this.name = newName;
        this.updatedAt = Instant.now();
    }

    /**
     * Met à jour le profil fiscal. Le matricule est contrôlé au format
     * s'il est fourni ; le régime SUSPENDU exige un n° d'attestation en cours
     * de validité (art. 11 Code TVA).
     */
    public void updateFiscalProfile(String matriculeFiscal, String patente, String address,
                                    FiscalRegime regime, String attestationNumber,
                                    LocalDate attestationDate, LocalDate expiryDate) {
        updateFiscalProfile(matriculeFiscal, patente, address, regime, attestationNumber,
                attestationDate, expiryDate, this.tejIdentifier);
    }

    public void updateFiscalProfile(String matriculeFiscal, String patente, String address,
                                    FiscalRegime regime, String attestationNumber,
                                    LocalDate attestationDate, LocalDate expiryDate,
                                    String tejIdentifier) {
        FiscalRegime effective = regime != null ? regime : FiscalRegime.ASSUJETTI;
        if (matriculeFiscal != null && !matriculeFiscal.isBlank()) {
            MatriculeFiscal.of(matriculeFiscal);
            this.matriculeFiscal = matriculeFiscal.trim().toUpperCase().replaceAll("\\s+", "");
        } else {
            this.matriculeFiscal = null;
        }
        this.patente = patente;
        this.tejIdentifier = tejIdentifier;
        this.address = address;
        if (effective == FiscalRegime.SUSPENDU) {
            if (attestationNumber == null || attestationNumber.isBlank()) {
                throw new DomainException("Le régime SUSPENDU exige un numéro d'attestation (art. 11 Code TVA)");
            }
            if (expiryDate != null && expiryDate.isBefore(LocalDate.now())) {
                throw new DomainException("L'attestation de suspension est expirée depuis le " + expiryDate);
            }
        }
        this.fiscalRegime = effective;
        this.suspensionAttestationNumber = attestationNumber;
        this.suspensionAttestationDate = attestationDate;
        this.suspensionExpiryDate = expiryDate;
        this.updatedAt = Instant.now();
    }

    /** Vrai si l'attestation de suspension est présente et non expirée. */
    public boolean hasValidSuspension() {
        if (fiscalRegime != FiscalRegime.SUSPENDU) {
            return false;
        }
        if (suspensionAttestationNumber == null || suspensionAttestationNumber.isBlank()) {
            return false;
        }
        return suspensionExpiryDate == null || !suspensionExpiryDate.isBefore(LocalDate.now());
    }

    public boolean isActive() {
        return status == OrganizationStatus.ACTIVE;
    }

    public boolean isSupplier() {
        return type == OrganizationType.SUPPLIER;
    }

    public boolean isShop() {
        return type == OrganizationType.SHOP;
    }

    public OrganizationId id() { return id; }
    public OrganizationName name() { return name; }
    public OrganizationType type() { return type; }
    public OrganizationStatus status() { return status; }
    public long version() { return version; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public String matriculeFiscal() { return matriculeFiscal; }
    public String patente() { return patente; }
    public String tejIdentifier() { return tejIdentifier; }
    public String defaultWithholdingNature() { return defaultWithholdingNature; }
    public void setDefaultWithholdingNature(String defaultWithholdingNature) {
        if (defaultWithholdingNature != null && !defaultWithholdingNature.isBlank()) {
            WithholdingNature.from(defaultWithholdingNature);
            this.defaultWithholdingNature = defaultWithholdingNature.trim().toUpperCase();
        } else {
            this.defaultWithholdingNature = null;
        }
        this.updatedAt = Instant.now();
    }
    public String address() { return address; }
    public FiscalRegime fiscalRegime() { return fiscalRegime != null ? fiscalRegime : FiscalRegime.ASSUJETTI; }
    public String suspensionAttestationNumber() { return suspensionAttestationNumber; }
    public LocalDate suspensionAttestationDate() { return suspensionAttestationDate; }
    public LocalDate suspensionExpiryDate() { return suspensionExpiryDate; }

    @Override
    public String toString() {
        return "Organization{id=" + id + ", name=" + name + ", type=" + type + ", status=" + status + "}";
    }
}
