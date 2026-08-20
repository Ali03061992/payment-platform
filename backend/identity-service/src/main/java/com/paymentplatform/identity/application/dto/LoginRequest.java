package com.paymentplatform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank(message = "Nom d'utilisateur requis") String username,
                           @NotBlank(message = "Mot de passe requis") String password) {
}