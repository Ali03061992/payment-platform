package com.paymentplatform.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username requis") String username,
        @NotBlank(message = "Email requis") @Email(message = "Email invalide") String email,
        @NotBlank(message = "Mot de passe requis") @Size(min = 8, max = 64) String password,
        @NotBlank(message = "Nom requis") String firstName,
        @NotBlank(message = "Prénom requis") String lastName,
        @Size(max = 30) String phone,
        @NotBlank(message = "Rôle requis") String role) {
}
