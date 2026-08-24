package com.paymentplatform.payment.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull(message = "L'ID de la boutique est obligatoire")
        Long shopId,

        @NotNull(message = "L'ID du fournisseur est obligatoire")
        Long supplierId,

        @NotNull(message = "Le montant est obligatoire")
        @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
        BigDecimal amount,

        @NotBlank(message = "La devise est obligatoire")
        String currency
) {}
