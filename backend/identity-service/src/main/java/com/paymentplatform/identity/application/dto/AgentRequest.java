package com.paymentplatform.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Création d'un agent (fournisseur ou boutique). */
public record AgentRequest(
        @NotBlank(message = "Nom requis") @Size(max = 100) String firstName,
        @NotBlank(message = "Prénom requis") @Size(max = 100) String lastName,
        @NotBlank(message = "Email requis") @Email(message = "Email invalide") String email,
        @Size(max = 30) String phone,
        @NotBlank(message = "Username requis") @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$",
                message = "Username invalide") String username,
        @NotBlank(message = "Rôle requis") String role,
        @Size(min = 8, max = 64, message = "Mot de passe entre 8 et 64 caractères") String password) {
}