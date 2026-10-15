package com.paymentplatform.organization.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Devis fournisseur versionné rattaché à une DEMANDE (pas à une commande).
 * Contenu figé en JSON ; l'acceptation crée la commande aux conditions figées.
 */
public final class SupplierQuote {

    private final UUID id;
    private final UUID requestId;
    private final String requestReference;
    private final UUID supplierId;
    private final int version;
    private final BigDecimal subtotal;
    private final BigDecimal taxAmount;
    private final BigDecimal total;
    private final BigDecimal stampAmount;
    private final String currency;
    private final String linesJson;
    private final UUID createdBy;
    private final Instant createdAt;

    private SupplierQuote(Builder b) {
        this.id = b.id;
        this.requestId = b.requestId;
        this.requestReference = b.requestReference;
        this.supplierId = b.supplierId;
        this.version = b.version;
        this.subtotal = b.subtotal;
        this.taxAmount = b.taxAmount;
        this.total = b.total;
        this.stampAmount = b.stampAmount;
        this.currency = b.currency;
        this.linesJson = b.linesJson;
        this.createdBy = b.createdBy;
        this.createdAt = b.createdAt;
    }

    public UUID id() { return id; }
    public UUID requestId() { return requestId; }
    public String requestReference() { return requestReference; }
    public UUID supplierId() { return supplierId; }
    public int version() { return version; }
    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal taxAmount() { return taxAmount; }
    public BigDecimal total() { return total; }
    public BigDecimal stampAmount() { return stampAmount; }
    public String currency() { return currency; }
    public String linesJson() { return linesJson; }
    public UUID createdBy() { return createdBy; }
    public Instant createdAt() { return createdAt; }

    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private UUID id;
        private UUID requestId;
        private String requestReference;
        private UUID supplierId;
        private int version;
        private BigDecimal subtotal = BigDecimal.ZERO;
        private BigDecimal taxAmount = BigDecimal.ZERO;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal stampAmount = BigDecimal.ZERO;
        private String currency = "TND";
        private String linesJson = "[]";
        private UUID createdBy;
        private Instant createdAt;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder requestId(UUID v) { this.requestId = v; return this; }
        public Builder requestReference(String v) { this.requestReference = v; return this; }
        public Builder supplierId(UUID v) { this.supplierId = v; return this; }
        public Builder version(int v) { this.version = v; return this; }
        public Builder subtotal(BigDecimal v) { this.subtotal = v; return this; }
        public Builder taxAmount(BigDecimal v) { this.taxAmount = v; return this; }
        public Builder total(BigDecimal v) { this.total = v; return this; }
        public Builder stampAmount(BigDecimal v) { this.stampAmount = v; return this; }
        public Builder currency(String v) { this.currency = v; return this; }
        public Builder linesJson(String v) { this.linesJson = v; return this; }
        public Builder createdBy(UUID v) { this.createdBy = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }

        public SupplierQuote build() {
            if (id == null || requestId == null || supplierId == null || createdBy == null) {
                throw new IllegalStateException("Devis incomplet");
            }
            if (createdAt == null) {
                createdAt = Instant.now();
            }
            return new SupplierQuote(this);
        }
    }
}
