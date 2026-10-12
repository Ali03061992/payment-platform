package com.paymentplatform.organization.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrganizationRequest(
        @NotBlank(message = "Le nom est requis") @Size(min = 2, max = 100) String name,
        String type,
        /** Matricule fiscal (format 1234567A/A/M/000). Obligatoire sauf particulier/export. */
        String matriculeFiscal,
        /** N° patente (contribution / registre), mention complémentaire sur facture. */
        String patente,
        String address,
        /** Régime fiscal : ASSUJETTI (défaut), FORFAITAIRE, SUSPENDU, EXPORT, NON_ASSUJETTI, ETAT_PUBLIC. */
        String fiscalRegime,
        /** N° attestation suspension TVA (art. 11) — obligatoire si SUSPENDU. */
        String suspensionAttestationNumber,
        java.time.LocalDate suspensionAttestationDate,
        java.time.LocalDate suspensionExpiryDate,
        /** Identifiant du compte TEJ (optionnel, dépôt des certificats RAS). */
        String tejIdentifier,
        /** Nature de retenue par défaut du fournisseur (ex. HONORAIRES_REEL). */
        String defaultWithholdingNature
) {
    public CreateOrganizationRequest(String name, String type) {
        this(name, type, null, null, null, null, null, null, null, null, null);
    }
}
