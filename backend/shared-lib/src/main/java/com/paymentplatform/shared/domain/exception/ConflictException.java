package com.paymentplatform.shared.domain.exception;

/** Conflit (état invalide, verrou optimiste, unicité) → HTTP 409. */
public class ConflictException extends DomainException {

    public ConflictException(String message) {
        super(message);
    }
}