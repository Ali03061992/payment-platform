package com.paymentplatform.organization.domain.valueobject;

import com.paymentplatform.shared.domain.exception.DomainException;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Taux de TVA tunisien légal par ligne de facture.
 * Taux en vigueur depuis le 01/01/2018 : 19% (normal), 13% (intermédiaire),
 * 7% (réduit). 0% réservé aux opérations exonérées/suspendues/export
 * (toujours avec motif textuel — art. 11 Code TVA).
 */
public record VatRate(BigDecimal value) {

    public static final BigDecimal RATE_STANDARD = new BigDecimal("19");
    public static final BigDecimal RATE_INTERMEDIATE = new BigDecimal("13");
    public static final BigDecimal RATE_REDUCED = new BigDecimal("7");
    public static final BigDecimal RATE_EXEMPT = BigDecimal.ZERO;

    private static final Set<String> LEGAL = Set.of("0", "7", "13", "19");

    public VatRate {
        if (value == null) {
            throw new DomainException("Le taux de TVA est requis");
        }
        String normalized = value.stripTrailingZeros().toPlainString();
        if (!LEGAL.contains(normalized)) {
            throw new DomainException("Taux de TVA illégal : " + value
                    + " (taux tunisiens : 19, 13, 7, et 0 avec motif d'exonération)");
        }
        value = new BigDecimal(normalized);
    }

    public static VatRate of(BigDecimal value) {
        if (value == null) {
            return standard();
        }
        return new VatRate(value);
    }

    public static VatRate standard() {
        return new VatRate(RATE_STANDARD);
    }

    public static VatRate exempt() {
        return new VatRate(RATE_EXEMPT);
    }

    public boolean isExempt() {
        return value.compareTo(BigDecimal.ZERO) == 0;
    }
}
