package com.paymentplatform.shared.domain.exception;

/**
 * M2 : panne infra en aval (timeout, service injoignable, 5xx, circuit ouvert).
 * Mappée en 503 — à distinguer des 409 métier (règle de gestion violée).
 */
public class ServiceUnavailableException extends DomainException {

    /**
     * Signale une panne d'aval avec message explicite (mappée en 503).
     *
     * @param message description de la panne
     */
    public ServiceUnavailableException(String message) {
        super(message);
    }

    /**
     * Signale une panne d'aval en conservant la cause d'origine.
     *
     * @param message description de la panne
     * @param cause cause sous-jacente (timeout, IO, 5xx)
     */
    public ServiceUnavailableException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
