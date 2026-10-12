package com.paymentplatform.organization.domain.model;

/**
 * Cycle de vie d'un certificat de retenue (TEJ), rattaché à la facture de commande :
 * READY (émis localement, à déposer) → DEPOSITED → ACCEPTED (référence TEJ)
 * ou REJECTED ; CANCELLED si la commande est annulée (cf. arrêté 10/05/2024).
 */
public enum WithholdingCertificateStatus {
    READY,
    DEPOSITED,
    ACCEPTED,
    REJECTED,
    CANCELLED;

    public boolean isTerminal() {
        return this == ACCEPTED || this == CANCELLED;
    }
}
