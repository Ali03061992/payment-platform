package com.paymentplatform.organization.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateRelationRequest(
        @NotNull(message = "L'ID du fournisseur est requis") UUID supplierId,
        @NotNull(message = "L'ID de la boutique est requis") UUID shopId
) {}
