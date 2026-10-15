package com.paymentplatform.organization.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * Devis fournisseur : lignes modifiées (quantités + remises lignes en %).
 * Aucune remise globale : seules les remises par produit s'appliquent.
 * La boutique ne saisit aucune remise (zéro forcé à la création).
 */
public record SendQuoteRequest(
        @NotEmpty(message = "Le devis doit contenir au moins un article")
        List<@Valid OrderItemRequest> items
) {}
