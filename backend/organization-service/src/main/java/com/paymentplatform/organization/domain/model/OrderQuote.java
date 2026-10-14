package com.paymentplatform.organization.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Devis versionné d'une commande (demande boutique → chiffrage fournisseur).
 * La boutique ne saisit aucune remise ; le fournisseur modifie les lignes,
 * pose les remises (lignes + globale %) et envoie le devis (notification shop).
 * Chaque envoi incrémente la version ; le contenu est figé en JSON.
 */
public final class OrderQuote {

    private final UUID id;
    private final UUID orderId;
    private final String orderReference;
    private final UUID supplierId;
    private final int version;
    private final BigDecimal globalDiscount;
    private final BigDecimal subtotal;
    private final BigDecimal taxAmount;
    private final BigDecimal total;
    private final BigDecimal stampAmount;
    private final String currency;
    private final String linesJson;
    private final UUID createdBy;
    private final Instant createdAt;

    private OrderQuote(Builder b) {
        this.id = b.id;
        this.orderId = b.orderId;
        this.orderReference = b.orderReference;
        this.supplierId = b.supplierId;
        this.version = b.version;
        this.globalDiscount = b.globalDiscount;
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
    public UUID orderId() { return orderId; }
    public String orderReference() { return orderReference; }
    public UUID supplierId() { return supplierId; }
    public int version() { return version; }
    public BigDecimal globalDiscount() { return globalDiscount; }
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
        private UUID orderId;
        private String orderReference;
        private UUID supplierId;
        private int version;
        private BigDecimal globalDiscount = BigDecimal.ZERO;
        private BigDecimal subtotal = BigDecimal.ZERO;
        private BigDecimal taxAmount = BigDecimal.ZERO;
        private BigDecimal total = BigDecimal.ZERO;
        private BigDecimal stampAmount = BigDecimal.ZERO;
        private String currency = "TND";
        private String linesJson = "[]";
        private UUID createdBy;
        private Instant createdAt;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder orderId(UUID v) { this.orderId = v; return this; }
        public Builder orderReference(String v) { this.orderReference = v; return this; }
        public Builder supplierId(UUID v) { this.supplierId = v; return this; }
        public Builder version(int v) { this.version = v; return this; }
        public Builder globalDiscount(BigDecimal v) { this.globalDiscount = v; return this; }
        public Builder subtotal(BigDecimal v) { this.subtotal = v; return this; }
        public Builder taxAmount(BigDecimal v) { this.taxAmount = v; return this; }
        public Builder total(BigDecimal v) { this.total = v; return this; }
        public Builder stampAmount(BigDecimal v) { this.stampAmount = v; return this; }
        public Builder currency(String v) { this.currency = v; return this; }
        public Builder linesJson(String v) { this.linesJson = v; return this; }
        public Builder createdBy(UUID v) { this.createdBy = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }

        public OrderQuote build() {
            if (id == null || orderId == null || supplierId == null || createdBy == null) {
                throw new IllegalStateException("Devis incomplet : id, commande, fournisseur et auteur requis");
            }
            if (createdAt == null) {
                createdAt = Instant.now();
            }
            return new OrderQuote(this);
        }
    }
}
