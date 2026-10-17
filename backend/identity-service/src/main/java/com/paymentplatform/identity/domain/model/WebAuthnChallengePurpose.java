package com.paymentplatform.identity.domain.model;

/** Usage attendu d'un challenge WebAuthn (registration vs authentication). */
public enum WebAuthnChallengePurpose {
    REGISTER,
    AUTHENTICATE
}
