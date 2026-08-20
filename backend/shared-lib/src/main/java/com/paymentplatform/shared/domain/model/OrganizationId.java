package com.paymentplatform.shared.domain.model;

/** Identifiant typé d'une organisation (fournisseur ou boutique). */
public record OrganizationId(long value) {

    public static OrganizationId of(long value) {
        return new OrganizationId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}