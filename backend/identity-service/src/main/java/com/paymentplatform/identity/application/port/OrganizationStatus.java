package com.paymentplatform.identity.application.port;

import java.util.UUID;

/** Statut d'une organisation (vue minimale, consommée via le Gateway). */
public record OrganizationStatus(UUID id, String type, String status) {

    public boolean isActive() {
        return "ACTIVE".equals(status);
    }
}