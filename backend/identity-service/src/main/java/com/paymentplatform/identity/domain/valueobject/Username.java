package com.paymentplatform.identity.domain.valueobject;

import com.paymentplatform.shared.domain.exception.DomainException;

import java.util.Locale;

/** Nom d'utilisateur : 3-50 caractères alphanumériques, point, tiret, underscore. */
public record Username(String value) {

    private static final String PATTERN = "^[a-zA-Z0-9._-]{3,50}$";

    public Username {
        if (value == null || !value.matches(PATTERN)) {
            throw new DomainException("Nom d'utilisateur invalide (3-50 caractères : lettres, chiffres, . _ -)");
        }
    }

    public static Username of(String value) {
        return new Username(value == null ? null : value.trim().toLowerCase(Locale.ROOT));
    }
}