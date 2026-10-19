package com.paymentplatform.organization.domain.model;

import com.paymentplatform.shared.domain.exception.DomainException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Journal des factures : une ligne par commande facturée, numérotée
 * séquentiellement par fournisseur et par année (FA-AAAA-NNNNNN).
 * L'émission est idempotente : la première génération PDF/TEIF attribue le
 * numéro, les régénérations réutilisent la même ligne.
 */
public class InvoiceJournal {

    private final UUID id;
    private final String invoiceNumber;
    private final UUID orderId;
    private final String orderReference;
    private final UUID supplierId;
    private final UUID shopId;
    private final String currency;
    private final BigDecimal totalTtc;
    private final BigDecimal vatAmount;
    private final Instant issuedAt;

    private InvoiceJournal(Builder b) {
        this.id = b.id;
        this.invoiceNumber = b.invoiceNumber;
        this.orderId = b.orderId;
        this.orderReference = b.orderReference;
        this.supplierId = b.supplierId;
        this.shopId = b.shopId;
        this.currency = b.currency;
        this.totalTtc = b.totalTtc;
        this.vatAmount = b.vatAmount;
        this.issuedAt = b.issuedAt;
    }

    /** Émission à partir d'une commande éligible (statut contrôlé par l'appelant). */
    public static InvoiceJournal fromOrder(Order order, String invoiceNumber) {
        return builder()
                .invoiceNumber(invoiceNumber)
                .orderId(order.getId())
                .orderReference(order.getReference())
                .supplierId(order.getSupplierId())
                .shopId(order.getShopId())
                .currency(order.getCurrency())
                .totalTtc(order.getTotal())
                .vatAmount(order.getTaxAmount())
                .build();
    }

    public UUID id() { return id; }
    public String invoiceNumber() { return invoiceNumber; }
    public UUID orderId() { return orderId; }
    public String orderReference() { return orderReference; }
    public UUID supplierId() { return supplierId; }
    public UUID shopId() { return shopId; }
    public String currency() { return currency; }
    public BigDecimal totalTtc() { return totalTtc; }
    public BigDecimal vatAmount() { return vatAmount; }
    public Instant issuedAt() { return issuedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private String invoiceNumber;
        private UUID orderId;
        private String orderReference;
        private UUID supplierId;
        private UUID shopId;
        private String currency;
        private BigDecimal totalTtc;
        private BigDecimal vatAmount;
        private Instant issuedAt;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder invoiceNumber(String v) { this.invoiceNumber = v; return this; }
        public Builder orderId(UUID v) { this.orderId = v; return this; }
        public Builder orderReference(String v) { this.orderReference = v; return this; }
        public Builder supplierId(UUID v) { this.supplierId = v; return this; }
        public Builder shopId(UUID v) { this.shopId = v; return this; }
        public Builder currency(String v) { this.currency = v; return this; }
        public Builder totalTtc(BigDecimal v) { this.totalTtc = v; return this; }
        public Builder vatAmount(BigDecimal v) { this.vatAmount = v; return this; }
        public Builder issuedAt(Instant v) { this.issuedAt = v; return this; }

        public InvoiceJournal build() {
            if (invoiceNumber == null || invoiceNumber.isBlank())
                throw new DomainException("Le numéro de facture est requis");
            if (orderId == null) throw new DomainException("La commande est requise");
            if (supplierId == null) throw new DomainException("Le fournisseur est requis");
            if (shopId == null) throw new DomainException("La boutique est requise");
            if (totalTtc == null) throw new DomainException("Le total TTC est requis");
            if (issuedAt == null) issuedAt = Instant.now();
            return new InvoiceJournal(this);
        }
    }
}
