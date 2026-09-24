package com.paymentplatform.organization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateDisputeRequest(
        @NotNull(message = "L'ID de la commande est requis") UUID orderId,
        @NotBlank(message = "Le motif est requis") String reason
) {}
