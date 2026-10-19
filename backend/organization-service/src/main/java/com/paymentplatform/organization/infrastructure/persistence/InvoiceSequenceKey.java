package com.paymentplatform.organization.infrastructure.persistence;

import java.io.Serializable;
import java.util.Objects;

/** Clé composite (fournisseur, année) du compteur de numérotation facture. */
public class InvoiceSequenceKey implements Serializable {

    private String supplierId;
    private Integer seqYear;

    public InvoiceSequenceKey() {}

    public InvoiceSequenceKey(String supplierId, Integer seqYear) {
        this.supplierId = supplierId;
        this.seqYear = seqYear;
    }

    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public Integer getSeqYear() { return seqYear; }
    public void setSeqYear(Integer seqYear) { this.seqYear = seqYear; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InvoiceSequenceKey key)) return false;
        return Objects.equals(supplierId, key.supplierId) && Objects.equals(seqYear, key.seqYear);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supplierId, seqYear);
    }
}
