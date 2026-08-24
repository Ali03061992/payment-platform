package com.paymentplatform.organization.application.dto;

import jakarta.validation.constraints.NotNull;

public record CreateRelationRequest(
        @NotNull(message = "L'ID du fournisseur est requis") Long supplierId,
        @NotNull(message = "L'ID de la boutique est requis") Long shopId
) {}
