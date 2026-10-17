package com.paymentplatform.identity.application.dto;

import org.springframework.security.web.webauthn.api.AuthenticatorAssertionResponse;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;

/** Assertion de connexion passkey (payload navigator.credentials.get). */
public record WebAuthnLoginRequest(
        PublicKeyCredential<AuthenticatorAssertionResponse> credential) {
}
