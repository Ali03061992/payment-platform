package com.paymentplatform.organization.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Demande de devis boutique (entité propre, séparée des commandes).
 * La boutique décrit son besoin SANS remise ; le fournisseur chiffre
 * (versions de devis) ; l'acceptation crée la commande aux conditions du devis.
 */
public final class QuoteRequest {

    private final UUID id;
    private final String reference;
    private final UUID supplierId;
    private final String supplierName;
    private final UUID shopId;
    private final String shopName;
    private final String notes;
    private QuoteRequestStatus status;
    private final String linesJson;
    private final UUID createdBy;
    private final Instant createdAt;

    private QuoteRequest(Builder b) {
        this.id = b.id;
        this.reference = b.reference;
        this.supplierId = b.supplierId;
        this.supplierName = b.supplierName;
        this.shopId = b.shopId;
        this.shopName = b.shopName;
        this.notes = b.notes;
        this.status = b.status;
        this.linesJson = b.linesJson;
        this.createdBy = b.createdBy;
        this.createdAt = b.createdAt;
    }

    public UUID id() { return id; }
    public String reference() { return reference; }
    public UUID supplierId() { return supplierId; }
    public String supplierName() { return supplierName; }
    public UUID shopId() { return shopId; }
    public String shopName() { return shopName; }
    public String notes() { return notes; }
    public QuoteRequestStatus status() { return status; }
    public String linesJson() { return linesJson; }
    public UUID createdBy() { return createdBy; }
    public Instant createdAt() { return createdAt; }

    public void markQuoted() {
        if (status == QuoteRequestStatus.PENDING) {
            status = QuoteRequestStatus.QUOTED;
        }
    }

    public void markAccepted() {
        if (status != QuoteRequestStatus.QUOTED && status != QuoteRequestStatus.PENDING) {
            throw new IllegalStateException("Demande non chiffrable en commande : " + status);
        }
        status = QuoteRequestStatus.ACCEPTED;
    }

    public void markCancelled() {
        if (status == QuoteRequestStatus.ACCEPTED) {
            throw new IllegalStateException("Demande déjà acceptée");
        }
        status = QuoteRequestStatus.CANCELLED;
    }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private String reference;
        private UUID supplierId;
        private String supplierName;
        private UUID shopId;
        private String shopName;
        private String notes;
        private QuoteRequestStatus status = QuoteRequestStatus.PENDING;
        private String linesJson = "[]";
        private UUID createdBy;
        private Instant createdAt;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder reference(String v) { this.reference = v; return this; }
        public Builder supplierId(UUID v) { this.supplierId = v; return this; }
        public Builder supplierName(String v) { this.supplierName = v; return this; }
        public Builder shopId(UUID v) { this.shopId = v; return this; }
        public Builder shopName(String v) { this.shopName = v; return this; }
        public Builder notes(String v) { this.notes = v; return this; }
        public Builder status(QuoteRequestStatus v) { this.status = v; return this; }
        public Builder linesJson(String v) { this.linesJson = v; return this; }
        public Builder createdBy(UUID v) { this.createdBy = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }

        public QuoteRequest build() {
            if (id == null || supplierId == null || shopId == null || createdBy == null) {
                throw new IllegalStateException("Demande incomplète");
            }
            if (createdAt == null) {
                createdAt = Instant.now();
            }
            return new QuoteRequest(this);
        }
    }
}
