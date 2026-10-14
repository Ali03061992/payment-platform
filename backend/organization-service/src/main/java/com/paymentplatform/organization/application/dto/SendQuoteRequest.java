package com.paymentplatform.organization.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

/**
 * Devis fournisseur : lignes modifiées (quantités + remises %) et remise
 * globale %. La boutique ne saisit aucune remise (zéro forcé à la création).
 */
public record SendQuoteRequest(
        @NotEmpty(message = "Le devis doit contenir au moins un article")
        List<@Valid OrderItemRequest> items,
        @DecimalMin(value = "0", message = "La remise globale doit être positive")
        @DecimalMax(value = "100", message = "La remise globale ne peut dépasser 100 %")
        BigDecimal globalDiscount
) {}
