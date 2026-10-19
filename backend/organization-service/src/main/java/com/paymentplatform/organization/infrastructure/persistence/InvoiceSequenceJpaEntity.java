package com.paymentplatform.organization.infrastructure.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

/** Compteur de numérotation facture par fournisseur et par année. */
@Entity
@Table(name = "invoice_sequence")
@IdClass(InvoiceSequenceKey.class)
public class InvoiceSequenceJpaEntity {

    @Id
    @Column(name = "supplier_id", columnDefinition = "VARCHAR(36)")
    private String supplierId;

    @Id
    @Column(name = "seq_year")
    private Integer seqYear;

    @Column(name = "last_number", nullable = false)
    private Integer lastNumber = 0;

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public Integer getSeqYear() { return seqYear; }
    public void setSeqYear(Integer seqYear) { this.seqYear = seqYear; }
    public Integer getLastNumber() { return lastNumber; }
    public void setLastNumber(Integer lastNumber) { this.lastNumber = lastNumber; }
}
