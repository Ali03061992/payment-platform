package com.paymentplatform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompletePasswordSetupRequest(
        @NotBlank(message = "Token requis") String token,
        @NotBlank(message = "Mot de passe requis") @Size(min = 8, max = 64, message = "Le mot de passe doit contenir entre 8 et 64 caractères") String newPassword
) {}
