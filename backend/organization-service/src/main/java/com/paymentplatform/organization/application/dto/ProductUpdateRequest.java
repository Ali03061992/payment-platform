package com.paymentplatform.organization.application.dto;

import java.math.BigDecimal;

public record ProductUpdateRequest(
    String name,
    String description,
    String imageUrl,
    BigDecimal unitPrice,
    Integer quantity,
    Integer minQuantity,
    String status,
    BigDecimal vatRate,
    Boolean fodecApplicable,
    String vatExemptionText,
    Boolean priceTtc
) {}
