package com.paymentplatform.organization.domain.model;

/**
 * Cycle de vie d'une demande de devis (découplée des commandes) :
 * PENDING (en attente de chiffrage) → QUOTED (≥1 devis) → ACCEPTED,
 * ou CANCELLED (annulée par la boutique).
 */
public enum QuoteRequestStatus {
    PENDING,
    QUOTED,
    ACCEPTED,
    CANCELLED
}
