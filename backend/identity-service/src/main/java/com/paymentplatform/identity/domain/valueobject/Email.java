package com.paymentplatform.identity.domain.valueobject;

import com.paymentplatform.shared.domain.exception.DomainException;

import java.util.Locale;

/** Adresse email valide (format simplifié RFC 5322). */
public record Email(String value) {

    private static final String PATTERN = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    public Email {
        if (value == null || !value.matches(PATTERN)) {
            throw new DomainException("Adresse email invalide");
        }
    }

    public static Email of(String value) {
        return new Email(value == null ? null : value.trim().toLowerCase(Locale.ROOT));
    }
}