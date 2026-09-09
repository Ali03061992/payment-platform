package com.paymentplatform.organization.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record CreateRelationRequest(
        @NotNull(message = "L'ID du fournisseur est requis") UUID supplierId,
        @NotNull(message = "L'ID de la boutique est requis") UUID shopId
) {}
