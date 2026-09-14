package com.paymentplatform.identity.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Mot de passe actuel requis") String currentPassword,
        @NotBlank(message = "Nouveau mot de passe requis") @Size(min = 8, max = 64, message = "Le mot de passe doit contenir entre 8 et 64 caractères") String newPassword
) {}
