package com.paymentplatform.shared.domain.exception;

/** Règle métier non satisfaite (relation inexistante, état invalide) → HTTP 422. */
public class UnprocessableEntityException extends DomainException {

    public UnprocessableEntityException(String message) {
        super(message);
    }
}