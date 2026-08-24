package com.paymentplatform.organization.domain.valueobject;

import com.paymentplatform.shared.domain.exception.DomainException;

public record OrganizationName(String value) {
    private static final int MIN_LENGTH = 2;
    private static final int MAX_LENGTH = 100;

    public OrganizationName {
        if (value == null || value.isBlank()) {
            throw new DomainException("Le nom de l'organisation est requis");
        }
        String trimmed = value.trim();
        if (trimmed.length() < MIN_LENGTH || trimmed.length() > MAX_LENGTH) {
            throw new DomainException("Le nom doit contenir entre " + MIN_LENGTH + " et " + MAX_LENGTH + " caractères");
        }
        value = trimmed;
    }

    public static OrganizationName of(String value) {
        return new OrganizationName(value);
    }
}
