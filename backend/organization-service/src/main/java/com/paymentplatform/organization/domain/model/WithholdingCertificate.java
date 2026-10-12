package com.paymentplatform.organization.domain.model;

import com.paymentplatform.shared.domain.exception.DomainException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Certificat de retenue à la source (art. 55 IRPP/IS, plateforme TEJ),
 * rattaché à la FACTURE DE COMMANDE (le paiement suit la facture).
 * Snapshot fiscal à la livraison : identités et MF des parties,
 * brut TTC, taux, RAS, net, référence/date/nature de l'opération, échéance légale.
 */
public class WithholdingCertificate {

    private final UUID id;
    private final UUID orderId;
    private final String number;
    private final UUID debtorId;
    private final String debtorName;
    private final String debtorMatricule;
    private final String debtorAddress;
    private final UUID beneficiaryId;
    private final String beneficiaryName;
    private final String beneficiaryMatricule;
    private final String beneficiaryAddress;
    private final WithholdingNature nature;
    private final BigDecimal grossAmount;
    private final BigDecimal rate;
    private final BigDecimal withholdingAmount;
    private final BigDecimal netAmount;
    private final String currency;
    private final String orderReference;
    private final LocalDate orderDate;
    private final String billingPeriod;
    private final LocalDate dueDate;
    private WithholdingCertificateStatus status;
    private String tejReference;
    private final Instant createdAt;
    private Instant updatedAt;

    private WithholdingCertificate(Builder b) {
        this.id = b.id;
        this.orderId = b.orderId;
        this.number = b.number;
        this.debtorId = b.debtorId;
        this.debtorName = b.debtorName;
        this.debtorMatricule = b.debtorMatricule;
        this.debtorAddress = b.debtorAddress;
        this.beneficiaryId = b.beneficiaryId;
        this.beneficiaryName = b.beneficiaryName;
        this.beneficiaryMatricule = b.beneficiaryMatricule;
        this.beneficiaryAddress = b.beneficiaryAddress;
        this.nature = b.nature;
        this.grossAmount = b.grossAmount;
        this.rate = b.rate;
        this.withholdingAmount = b.withholdingAmount;
        this.netAmount = b.netAmount;
        this.currency = b.currency;
        this.orderReference = b.orderReference;
        this.orderDate = b.orderDate;
        this.billingPeriod = b.billingPeriod;
        this.dueDate = b.dueDate;
        this.status = b.status;
        this.tejReference = b.tejReference;
        this.createdAt = b.createdAt;
        this.updatedAt = b.updatedAt;
    }

    public void cancel() {
        if (status.isTerminal()) {
            throw new DomainException("Le certificat " + number + " est terminal (" + status + ")");
        }
        this.status = WithholdingCertificateStatus.CANCELLED;
        this.updatedAt = Instant.now();
    }

    /** Phase 2 : accusé de dépôt TEJ (référence plateforme). */
    public void markDeposited(String tejReference) {
        if (status != WithholdingCertificateStatus.READY && status != WithholdingCertificateStatus.REJECTED) {
            throw new DomainException("Dépôt impossible dans l'état " + status);
        }
        this.tejReference = tejReference;
        this.status = WithholdingCertificateStatus.DEPOSITED;
        this.updatedAt = Instant.now();
    }

    public UUID id() { return id; }
    public UUID orderId() { return orderId; }
    public String number() { return number; }
    public UUID debtorId() { return debtorId; }
    public String debtorName() { return debtorName; }
    public String debtorMatricule() { return debtorMatricule; }
    public String debtorAddress() { return debtorAddress; }
    public UUID beneficiaryId() { return beneficiaryId; }
    public String beneficiaryName() { return beneficiaryName; }
    public String beneficiaryMatricule() { return beneficiaryMatricule; }
    public String beneficiaryAddress() { return beneficiaryAddress; }
    public WithholdingNature nature() { return nature; }
    public BigDecimal grossAmount() { return grossAmount; }
    public BigDecimal rate() { return rate; }
    public BigDecimal withholdingAmount() { return withholdingAmount; }
    public BigDecimal netAmount() { return netAmount; }
    public String currency() { return currency; }
    public String orderReference() { return orderReference; }
    public LocalDate orderDate() { return orderDate; }
    public String billingPeriod() { return billingPeriod; }
    public LocalDate dueDate() { return dueDate; }
    public WithholdingCertificateStatus status() { return status; }
    public String tejReference() { return tejReference; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private UUID id;
        private UUID orderId;
        private String number;
        private UUID debtorId;
        private String debtorName;
        private String debtorMatricule;
        private String debtorAddress;
        private UUID beneficiaryId;
        private String beneficiaryName;
        private String beneficiaryMatricule;
        private String beneficiaryAddress;
        private WithholdingNature nature;
        private BigDecimal grossAmount;
        private BigDecimal rate;
        private BigDecimal withholdingAmount;
        private BigDecimal netAmount;
        private String currency;
        private String orderReference;
        private LocalDate orderDate;
        private String billingPeriod;
        private LocalDate dueDate;
        private WithholdingCertificateStatus status;
        private String tejReference;
        private Instant createdAt;
        private Instant updatedAt;

        public Builder id(UUID v) { this.id = v; return this; }
        public Builder orderId(UUID v) { this.orderId = v; return this; }
        public Builder number(String v) { this.number = v; return this; }
        public Builder debtorId(UUID v) { this.debtorId = v; return this; }
        public Builder debtorName(String v) { this.debtorName = v; return this; }
        public Builder debtorMatricule(String v) { this.debtorMatricule = v; return this; }
        public Builder debtorAddress(String v) { this.debtorAddress = v; return this; }
        public Builder beneficiaryId(UUID v) { this.beneficiaryId = v; return this; }
        public Builder beneficiaryName(String v) { this.beneficiaryName = v; return this; }
        public Builder beneficiaryMatricule(String v) { this.beneficiaryMatricule = v; return this; }
        public Builder beneficiaryAddress(String v) { this.beneficiaryAddress = v; return this; }
        public Builder nature(WithholdingNature v) { this.nature = v; return this; }
        public Builder grossAmount(BigDecimal v) { this.grossAmount = v; return this; }
        public Builder rate(BigDecimal v) { this.rate = v; return this; }
        public Builder withholdingAmount(BigDecimal v) { this.withholdingAmount = v; return this; }
        public Builder netAmount(BigDecimal v) { this.netAmount = v; return this; }
        public Builder currency(String v) { this.currency = v; return this; }
        public Builder orderReference(String v) { this.orderReference = v; return this; }
        public Builder orderDate(LocalDate v) { this.orderDate = v; return this; }
        public Builder billingPeriod(String v) { this.billingPeriod = v; return this; }
        public Builder dueDate(LocalDate v) { this.dueDate = v; return this; }
        public Builder status(WithholdingCertificateStatus v) { this.status = v; return this; }
        public Builder tejReference(String v) { this.tejReference = v; return this; }
        public Builder createdAt(Instant v) { this.createdAt = v; return this; }
        public Builder updatedAt(Instant v) { this.updatedAt = v; return this; }

        public WithholdingCertificate build() {
            if (orderId == null) throw new DomainException("La commande est requise");
            if (number == null) throw new DomainException("Le numéro de certificat est requis");
            if (withholdingAmount == null || withholdingAmount.signum() <= 0)
                throw new DomainException("Un certificat exige une retenue strictement positive");
            if (status == null) status = WithholdingCertificateStatus.READY;
            Instant now = Instant.now();
            if (createdAt == null) createdAt = now;
            if (updatedAt == null) updatedAt = now;
            return new WithholdingCertificate(this);
        }
    }
}
