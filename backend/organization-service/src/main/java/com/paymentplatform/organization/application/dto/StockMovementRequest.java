package com.paymentplatform.organization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockMovementRequest(
    @NotNull Long productId,
    @NotBlank String type,
    @NotNull @Positive Integer quantity,
    String reference,
    String notes
) {}
