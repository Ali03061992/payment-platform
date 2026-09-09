package com.paymentplatform.organization.application.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockMovementRequest(
    @NotNull UUID productId,
    @NotBlank String type,
    @NotNull @Positive Integer quantity,
    String reference,
    String notes
) {}
