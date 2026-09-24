package com.paymentplatform.payment.domain.model;

import java.math.BigDecimal;

/**
 * B5 : agrégat SQL (COUNT/SUM) par statut — évite de charger tous les
 * paiements en mémoire pour le supplier-summary.
 */
public record PaymentStatusSummary(PaymentStatus status, long count, BigDecimal total) {
}
