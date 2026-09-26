package com.paymentplatform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

public record DevLoginRequest(
        @NotBlank(message = "Nom d'utilisateur requis") String username) {
}
