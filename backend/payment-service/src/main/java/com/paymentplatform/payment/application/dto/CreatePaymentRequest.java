package com.paymentplatform.payment.application.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull(message = "L'ID de la boutique est obligatoire")
        UUID shopId,

        @NotNull(message = "L'ID du fournisseur est obligatoire")
        UUID supplierId,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
        @DecimalMax(value = "999999.99", message = "Le montant ne doit pas dépasser 999999.99")
        BigDecimal amount,

        @NotBlank(message = "La devise est obligatoire")
        String currency,

        UUID orderId,
        LocalDate dueDate
) {}
