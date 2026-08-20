package com.paymentplatform.identity.application.port;

/** Statut d'une organisation (vue minimale, consommée via le Gateway). */
public record OrganizationStatus(long id, String type, String status) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}