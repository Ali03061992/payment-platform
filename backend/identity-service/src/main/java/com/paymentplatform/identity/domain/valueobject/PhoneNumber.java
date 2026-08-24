package com.paymentplatform.identity.domain.valueobject;

import com.paymentplatform.shared.domain.exception.DomainException;

/** Numéro de téléphone optionnel, format E.164. Préfixe Tunisie +216 automatique. */
public record PhoneNumber(String value) {

    private static final String E164_PATTERN = "^\\+?[0-9]{8,15}$";
    private static final String TUNISIA_PREFIX = "+216";

    public PhoneNumber {
        if (value != null && !value.isBlank()) {
            String normalized = value.trim();
            if (!normalized.startsWith("+")) {
                if (normalized.startsWith("0")) {
                    normalized = normalized.substring(1);
                }
                normalized = TUNISIA_PREFIX + normalized;
            }
            if (!normalized.matches(E164_PATTERN)) {
                throw new DomainException("Numéro de téléphone invalide");
            }
            value = normalized;
        } else {
            value = null;
        }
    }

    public static PhoneNumber of(String value) {
        return new PhoneNumber(value);
    }
}