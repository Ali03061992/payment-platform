package com.paymentplatform.organization.domain.model;

/**
 * Régime fiscal d'une organisation au regard de la TVA tunisienne.
 *
 * <ul>
 *   <li>ASSUJETTI — cas général, TVA collectée et déductible, MF à code A/P/B/D.</li>
 *   <li>FORFAITAIRE — régime forfaitaire : pas de facture TVA, pas de collecte, pas de déduction (art. 18 §II).</li>
 *   <li>SUSPENDU — achats en suspension de TVA sur attestation (art. 11) : vente à 0% avec mention obligatoire.</li>
 *   <li>EXPORT — client étranger / vente à l'exportation : 0%, mention « vente à l'exportation », sans timbre.</li>
 *   <li>NON_ASSUJETTI — particulier : MF non exigé, TVA du fournisseur reste due.</li>
 *   <li>ETAT_PUBLIC — État, collectivités, établissements publics : retenue TVA 50% (art. 19bis, P1).</li>
 * </ul>
 */
public enum FiscalRegime {
    ASSUJETTI,
    FORFAITAIRE,
    SUSPENDU,
    EXPORT,
    NON_ASSUJETTI,
    ETAT_PUBLIC;

    /** Vrai si une vente vers ce régime se fait à 0% avec mention légale (art. 11). */
    public boolean isVatExemptSale() {
        return this == SUSPENDU || this == EXPORT;
    }

    /** Vrai si le matricule fiscal de l'acheteur n'est pas exigible sur la facture. */
    public boolean isBuyerMatriculeOptional() {
        return this == NON_ASSUJETTI || this == FORFAITAIRE || this == EXPORT;
    }

    public static FiscalRegime from(String value) {
        if (value == null || value.isBlank()) {
            return ASSUJETTI;
        }
        try {
            return FiscalRegime.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new com.paymentplatform.shared.domain.exception.DomainException(
                    "Régime fiscal inconnu : " + value + " (attendus : ASSUJETTI, FORFAITAIRE, SUSPENDU, EXPORT, NON_ASSUJETTI, ETAT_PUBLIC)");
        }
    }
}
