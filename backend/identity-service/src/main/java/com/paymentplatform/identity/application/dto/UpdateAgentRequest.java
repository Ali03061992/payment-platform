package com.paymentplatform.identity.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAgentRequest(
        @NotBlank(message = "Nom requis") @Size(max = 100) String firstName,
        @NotBlank(message = "Prénom requis") @Size(max = 100) String lastName,
        @Email(message = "Email invalide") String email,
        @Size(max = 30) String phone) {
}