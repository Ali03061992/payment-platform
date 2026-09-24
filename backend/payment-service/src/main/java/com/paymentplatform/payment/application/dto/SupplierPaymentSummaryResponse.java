package com.paymentplatform.payment.application.dto;

import java.math.BigDecimal;

/**
 * B5 : mêmes clés JSON que l'ancien calcul en mémoire (contrat front inchangé),
 * mais totaux issus d'agrégats SQL (aucun chargement des paiements).
 */
public record SupplierPaymentSummaryResponse(
        BigDecimal confirmedTotal,
        long confirmedCount,
        BigDecimal pendingTotal,
        long pendingCount,
        BigDecimal rejectedTotal,
        long rejectedCount
) {
}
