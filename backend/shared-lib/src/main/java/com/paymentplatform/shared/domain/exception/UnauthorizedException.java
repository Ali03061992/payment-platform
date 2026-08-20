package com.paymentplatform.shared.domain.exception;

/** Non authentifié → HTTP 401. */
public class UnauthorizedException extends DomainException {

    public UnauthorizedException(String message) {
        super(message);
    }
}