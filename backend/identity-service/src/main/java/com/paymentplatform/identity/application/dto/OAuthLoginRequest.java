package com.paymentplatform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

public record OAuthLoginRequest(
        @NotBlank(message = "Fournisseur requis") String provider,
        @NotBlank(message = "Jeton requis") String idToken) {
}
