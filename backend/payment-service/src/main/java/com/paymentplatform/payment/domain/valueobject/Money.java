package com.paymentplatform.payment.domain.valueobject;

import com.paymentplatform.shared.domain.exception.DomainException;

import java.math.BigDecimal;
import java.util.Objects;

public record Money(BigDecimal amount, String currency) {

    public Money {
        Objects.requireNonNull(amount, "Le montant est obligatoire");
        Objects.requireNonNull(currency, "La devise est obligatoire");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new DomainException("Le montant doit être supérieur à 0");
        }
        if (currency.isBlank()) {
            throw new DomainException("La devise ne peut pas être vide");
        }
        if (currency.length() != 3) {
            throw new DomainException("La devise doit être un code ISO 3 lettres");
        }
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount.stripTrailingZeros(), currency.toUpperCase());
    }
}
