package com.paymentplatform.organization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ProductCreateRequest(
    @NotBlank String name,
    @NotBlank String sku,
    String description,
    @NotNull @PositiveOrZero BigDecimal unitPrice,
    @NotBlank String currency,
    @NotNull @PositiveOrZero Integer quantity,
    @NotNull @PositiveOrZero Integer minQuantity
) {}
