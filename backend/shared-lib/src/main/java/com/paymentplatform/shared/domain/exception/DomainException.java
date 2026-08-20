package com.paymentplatform.shared.domain.exception;

/** Exception métier générique : le message est exposé au client. */
public class DomainException extends RuntimeException {

    public DomainException(String message) {
        super(message);
    }
}