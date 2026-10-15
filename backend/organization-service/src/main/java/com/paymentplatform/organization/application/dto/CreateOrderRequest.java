package com.paymentplatform.organization.application.dto;

import com.paymentplatform.organization.domain.model.PaymentTerms;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(
        @NotNull(message = "L'ID du fournisseur est requis") UUID supplierId,
        @NotNull(message = "L'ID de la boutique est requis") UUID shopId,
        Boolean asapPayment,
        PaymentTerms paymentTerms,
        @NotBlank(message = "La devise est requise") String currency,
        /* @deprecated Taux global historique IGNORÉ : la TVA est portée par chaque
           produit (Product.vatRate 19/13/7/0, art. 18 Code TVA). Conservé pour
           compatibilité ascendante des clients API. */
        @Min(value = 0, message = "Le taux de TVA doit être entre 0 et 100")
        @Max(value = 100, message = "Le taux de TVA doit être entre 0 et 100")
        BigDecimal taxRate,
        String notes,
        @NotEmpty(message = "La commande doit contenir au moins un article") List<OrderItemRequest> items
) {}
