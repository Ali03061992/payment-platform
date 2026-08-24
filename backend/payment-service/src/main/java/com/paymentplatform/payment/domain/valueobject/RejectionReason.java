package com.paymentplatform.payment.domain.valueobject;

import com.paymentplatform.shared.domain.exception.DomainException;

import java.util.Objects;

public record RejectionReason(String value) {

    public RejectionReason {
        Objects.requireNonNull(value, "Le motif de rejet est obligatoire");
        if (value.isBlank()) {
            throw new DomainException("Le motif de rejet ne peut pas être vide");
        }
        if (value.length() > 500) {
            throw new DomainException("Le motif de rejet ne peut pas dépasser 500 caractères");
        }
    }
}
