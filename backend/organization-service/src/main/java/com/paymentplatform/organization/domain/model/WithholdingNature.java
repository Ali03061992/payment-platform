package com.paymentplatform.organization.domain.model;

import com.paymentplatform.shared.domain.exception.DomainException;

import java.math.BigDecimal;

/**
 * Nature du montant facturé → taux de retenue à la source (art. 52 IRPP/IS).
 * Base : montant TTC de la facture (seuil 1 000 DT pour les marchandises).
 * Le certificat est rattaché à la FACTURE DE COMMANDE (le paiement suit la facture).
 */
public enum WithholdingNature {
    /** Achats marchandises, matériel, équipements, services : 1,5% si ≥ 1000 DT TTC. */
    MARCHANDISES(new BigDecimal("1.5"), new BigDecimal("1000")),
    /** Honoraires versés à une personne morale IS à comptabilité conforme : 3%. */
    HONORAIRES_REEL(new BigDecimal("3"), null),
    /** Honoraires régime forfaitaire, commissions, courtages, loyers, non-commercial : 10%. */
    HONORAIRES_FORFAIT(new BigDecimal("10"), null),
    /** Rémunérations de non-résidents non établis (sauf convention) : 15%. */
    NON_RESIDENT(new BigDecimal("15"), null),
    /** Intérêts, revenus de capitaux mobiliers, jetons de présence : 20%. */
    INTERETS(new BigDecimal("20"), null),
    /** Dividendes : 10%. */
    DIVIDENDES(new BigDecimal("10"), null),
    /** Plus-value immobilière (personne physique) : 2,5%. */
    PLUS_VALUE_IMMO(new BigDecimal("2.5"), null),
    /** Bénéficiaire sans matricule fiscal : 15%. */
    SANS_MF(new BigDecimal("15"), null);

    private final BigDecimal rate;
    private final BigDecimal thresholdTtc;

    WithholdingNature(BigDecimal rate, BigDecimal thresholdTtc) {
        this.rate = rate;
        this.thresholdTtc = thresholdTtc;
    }

    public BigDecimal rate() { return rate; }

    /**
     * Taux applicable compte tenu du seuil (marchandises < 1000 TTC → 0%).
     */
    public BigDecimal applicableRate(BigDecimal grossTtc) {
        if (thresholdTtc != null && (grossTtc == null || grossTtc.compareTo(thresholdTtc) < 0)) {
            return BigDecimal.ZERO;
        }
        return rate;
    }

    public static WithholdingNature from(String value) {
        if (value == null || value.isBlank()) {
            return MARCHANDISES;
        }
        try {
            return WithholdingNature.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new DomainException("Nature de retenue inconnue : " + value
                    + " (attendues : MARCHANDISES, HONORAIRES_REEL, HONORAIRES_FORFAIT, "
                    + "NON_RESIDENT, INTERETS, DIVIDENDES, PLUS_VALUE_IMMO, SANS_MF)");
        }
    }
}
