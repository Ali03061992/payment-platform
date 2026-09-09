package com.paymentplatform.identity.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Création d'un utilisateur par un service interne (ex. admin initial d'une organisation). */
public record CreateInternalUserRequest(
        @NotBlank(message = "Username requis") String username,
        @NotBlank(message = "Email requis") @Email(message = "Email invalide") String email,
        @Size(min = 8, max = 64) String password,
        @NotBlank(message = "Nom requis") String firstName,
        @NotBlank(message = "Prénom requis") String lastName,
        @Size(max = 30) String phone,
        UUID organizationId,
        @NotBlank(message = "Rôle requis") String role) {
}