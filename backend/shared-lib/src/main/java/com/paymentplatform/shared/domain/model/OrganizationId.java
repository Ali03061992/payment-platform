package com.paymentplatform.shared.domain.model;

import java.util.UUID;

/** Identifiant typé d'une organisation (fournisseur ou boutique). */
public record OrganizationId(UUID value) {

    public static OrganizationId of(UUID value) {
        return new OrganizationId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}