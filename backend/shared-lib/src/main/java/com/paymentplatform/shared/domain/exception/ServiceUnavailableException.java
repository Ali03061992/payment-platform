package com.paymentplatform.shared.domain.exception;

/**
 * M2 : panne infra en aval (timeout, service injoignable, 5xx, circuit ouvert).
 * Mappée en 503 — à distinguer des 409 métier (règle de gestion violée).
 */
public class ServiceUnavailableException extends DomainException {

    public ServiceUnavailableException(String message) {
        super(message);
    }

    public ServiceUnavailableException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
