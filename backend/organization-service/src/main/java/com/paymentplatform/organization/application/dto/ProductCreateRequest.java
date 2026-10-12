package com.paymentplatform.organization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ProductCreateRequest(
    @NotBlank String name,
    @NotBlank String sku,
    String description,
    String imageUrl,
    @NotNull @PositiveOrZero BigDecimal unitPrice,
    @NotBlank String currency,
    @NotNull @PositiveOrZero Integer quantity,
    @NotNull @PositiveOrZero Integer minQuantity,
    /** Taux TVA tunisien du produit : 19, 13, 7 (0 = exonéré avec motif). Défaut 19. */
    BigDecimal vatRate,
    /** Soumis au FODEC 1% (produits industriels). Défaut false. */
    Boolean fodecApplicable,
    /** Motif d'exonération obligatoire si vatRate = 0. */
    String vatExemptionText,
    /** Prix de vente TTC (TVA incluse, non ajoutée). Défaut false = prix HT. */
    Boolean priceTtc
) {}
