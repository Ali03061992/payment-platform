package com.paymentplatform.identity.application.dto;

import org.springframework.security.web.webauthn.api.AuthenticatorAttestationResponse;
import org.springframework.security.web.webauthn.api.PublicKeyCredential;

/** Confirmation d'enregistrement d'une passkey (payload navigator.credentials.create). */
public record WebAuthnRegisterRequest(
        PublicKeyCredential<AuthenticatorAttestationResponse> credential,
        String label) {
}
