package com.paymentplatform.identity.domain.model;

/** Fournisseur d'authentification (LOCAL = mot de passe, sinon OIDC). */
public enum AuthProvider {
    LOCAL,
    GOOGLE,
    MICROSOFT;

    public static AuthProvider from(String value) {
        if (value == null || value.isBlank()) {
            return LOCAL;
        }
        try {
            return AuthProvider.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Fournisseur d'authentification inconnu : " + value);
        }
    }
}
