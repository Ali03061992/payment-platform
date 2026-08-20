package com.paymentplatform.shared.domain.exception;

/** Ressource introuvable → HTTP 404. */
public class NotFoundException extends DomainException {

    public NotFoundException(String message) {
        super(message);
    }
}