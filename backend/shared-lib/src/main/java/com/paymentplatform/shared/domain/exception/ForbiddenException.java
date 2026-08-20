package com.paymentplatform.shared.domain.exception;

/** Droits insuffisants ou hors périmètre → HTTP 403. */
public class ForbiddenException extends DomainException {

    public ForbiddenException(String message) {
        super(message);
    }
}