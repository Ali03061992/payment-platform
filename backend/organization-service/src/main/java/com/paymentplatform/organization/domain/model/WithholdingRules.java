package com.paymentplatform.organization.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * Règles de calcul et d'échéance des certificats de retenue à la source.
 * Références : art. 52 (taux) et 55 (mentions, certificat) IRPP/IS,
 * arrêté du 10 mai 2024 (réf/date/nature de l'opération, délai fin du mois suivant).
 * Assiette : montant TTC de la FACTURE DE COMMANDE.
 */
public final class WithholdingRules {

    private WithholdingRules() {}

    public static final int SCALE = 4;

    public record Computation(BigDecimal rate, BigDecimal withholding, BigDecimal net) {}

    /**
     * Résout la nature effective : sans MF bénéficiaire → SANS_MF 15%.
     */
    public static WithholdingNature effectiveNature(WithholdingNature declared, String beneficiaryMatricule) {
        if (beneficiaryMatricule == null || beneficiaryMatricule.isBlank()) {
            return WithholdingNature.SANS_MF;
        }
        return declared != null ? declared : WithholdingNature.MARCHANDISES;
    }

    public static Computation compute(BigDecimal grossTtc, WithholdingNature nature) {
        BigDecimal gross = grossTtc != null ? grossTtc : BigDecimal.ZERO;
        BigDecimal rate = nature != null ? nature.applicableRate(gross) : BigDecimal.ZERO;
        BigDecimal withholding = scale(gross.multiply(rate).divide(new BigDecimal("100"), SCALE, RoundingMode.HALF_UP));
        return new Computation(rate, withholding, scale(gross.subtract(withholding)));
    }

    /**
     * Échéance légale : fin du mois suivant la facturation
     * (facture de juillet → 31 août).
     */
    public static LocalDate dueDate(LocalDate invoiceDate) {
        LocalDate base = invoiceDate != null ? invoiceDate : LocalDate.now();
        return base.plusMonths(1).withDayOfMonth(base.plusMonths(1).lengthOfMonth());
    }

    /** Numéro de certificat : CRT-AAAA MM - séquence courte unique. */
    public static String certificateNumber() {
        LocalDate now = LocalDate.now();
        String seq = java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return "CRT-" + now.getYear() + String.format("%02d", now.getMonthValue()) + "-" + seq;
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }
}
