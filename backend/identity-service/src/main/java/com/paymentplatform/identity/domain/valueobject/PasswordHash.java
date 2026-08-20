package com.paymentplatform.identity.domain.valueobject;

/** Hash BCrypt du mot de passe. Le mot de passe en clair ne quitte jamais le domaine. */
public record PasswordHash(String value) {

    public PasswordHash {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Hash de mot de passe manquant");
        }
    }

    public static PasswordHash of(String encoded) {
        return new PasswordHash(encoded);
    }
}