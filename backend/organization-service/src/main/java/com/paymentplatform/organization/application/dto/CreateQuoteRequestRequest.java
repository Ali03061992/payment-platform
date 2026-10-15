package com.paymentplatform.organization.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Demande de devis boutique : besoin SANS remise (zéro forcé côté serveur).
 */
public record CreateQuoteRequestRequest(
        @NotNull(message = "Le fournisseur est requis") UUID supplierId,
        String notes,
        @NotEmpty(message = "La demande doit contenir au moins un article")
        List<@Valid QuoteRequestLine> items
) {
    public record QuoteRequestLine(
            @NotNull(message = "L'ID du produit est requis") UUID productId,
            @NotNull(message = "La quantité est requise") Integer quantity
    ) {}
}
