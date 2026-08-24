package com.paymentplatform.payment.domain.model;

import com.paymentplatform.shared.domain.exception.DomainException;

public enum PaymentStatus {
    PENDING, CONFIRMED, REJECTED, CANCELLED;

    public boolean isTerminal() {
        return this == CONFIRMED || this == REJECTED || this == CANCELLED;
    }

    public void assertCanTransitionTo(PaymentStatus target) {
        if (this == PENDING && (target == CONFIRMED || target == REJECTED || target == CANCELLED)) {
            return;
        }
        if (this.isTerminal()) {
            throw new DomainException("Le paiement est dans un état terminal (" + this
                    + ") et ne peut pas être transitionné vers " + target);
        }
        throw new DomainException("Transition invalide de " + this + " vers " + target);
    }

    public static PaymentStatus from(String value) {
        try {
            return PaymentStatus.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("Statut de paiement inconnu : " + value);
        }
    }
}
