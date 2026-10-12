package com.paymentplatform.organization.application.dto;

import java.util.UUID;

public record OrganizationStatusResponse(
        UUID id,
        String name,
        String type,
        String status,
        String matriculeFiscal,
        String patente,
        String address,
        String fiscalRegime,
        String suspensionAttestationNumber,
        java.time.LocalDate suspensionAttestationDate,
        java.time.LocalDate suspensionExpiryDate,
        String tejIdentifier,
        String defaultWithholdingNature
) {
    /** Compatibilité : statut seul (sans profil fiscal). */
    public OrganizationStatusResponse(UUID id, String name, String type, String status) {
        this(id, name, type, status, null, null, null, null, null, null, null, null, null);
    }
}
