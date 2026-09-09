package com.paymentplatform.shared.domain.model;

import java.util.UUID;

/** Identifiant typé d'un utilisateur. */
public record UserId(UUID value) {

    public static UserId of(UUID value) {
        return new UserId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}