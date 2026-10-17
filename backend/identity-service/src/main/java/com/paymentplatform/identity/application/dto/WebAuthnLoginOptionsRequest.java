package com.paymentplatform.identity.application.dto;

/** Demande d'options de connexion passkey (username-first, nullable = discoverable). */
public record WebAuthnLoginOptionsRequest(String username) {
}
