package com.paymentplatform.identity.domain.valueobject;

import com.paymentplatform.shared.domain.exception.DomainException;

/** Numéro de téléphone optionnel, format E.164. */
public record PhoneNumber(String value) {

    private static final String PATTERN = "^\\+?[0-9]{8,15}$";

    public PhoneNumber {
        if (value != null && !value.isBlank() && !value.matches(PATTERN)) {
            throw new DomainException("Numéro de téléphone invalide");
        }
        if (value == null || value.isBlank()) {
            value = null;
        }
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }
}