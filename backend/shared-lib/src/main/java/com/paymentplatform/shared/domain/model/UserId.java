package com.paymentplatform.shared.domain.model;

/** Identifiant typé d'un utilisateur. */
public record UserId(long value) {

    public static UserId of(long value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}