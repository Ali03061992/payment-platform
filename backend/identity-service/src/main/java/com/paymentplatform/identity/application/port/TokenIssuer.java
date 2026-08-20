package com.paymentplatform.identity.application.port;

import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;

/** Émission de jetons JWT (implémenté par l'infrastructure via shared-lib). */
public interface TokenIssuer {

    String issue(AuthenticatedUser user);

    long expirationSeconds();
}