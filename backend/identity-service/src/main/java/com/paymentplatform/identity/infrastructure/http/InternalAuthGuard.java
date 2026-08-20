package com.paymentplatform.identity.infrastructure.http;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** Garde des appels internes entre microservices (header X-Internal-Token). */
@Component
public class InternalAuthGuard {

    private final String internalSecret;

    public InternalAuthGuard(@Value("${app.internal-secret:}") String internalSecret) {
        this.internalSecret = internalSecret;
    }

    public boolean isValid(String token) {
        return !internalSecret.isBlank() && internalSecret.equals(token);
    }
}