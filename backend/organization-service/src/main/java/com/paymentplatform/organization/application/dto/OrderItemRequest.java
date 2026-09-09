package com.paymentplatform.organization.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotNull(message = "L'ID du produit est requis") UUID productId,
        @NotNull(message = "La quantité est requise") @Min(value = 1, message = "La quantité doit être au moins 1") Integer quantity,
        BigDecimal discount
) {}
