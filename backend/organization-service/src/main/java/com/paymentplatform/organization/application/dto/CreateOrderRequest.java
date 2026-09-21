package com.paymentplatform.organization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "L'ID du fournisseur est requis") UUID supplierId,
        @NotNull(message = "L'ID de la boutique est requis") UUID shopId,
        Boolean asapPayment,
        @NotBlank(message = "La devise est requise") String currency,
        String notes,
        @NotEmpty(message = "La commande doit contenir au moins un article") List<OrderItemRequest> items
) {}
