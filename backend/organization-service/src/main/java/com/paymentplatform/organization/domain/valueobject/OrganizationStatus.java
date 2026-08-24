package com.paymentplatform.organization.domain.valueobject;

public enum OrganizationStatus {
    ACTIVE,
    DISABLED;

    public static OrganizationStatus from(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new com.paymentplatform.shared.domain.exception.DomainException(
                    "Statut invalide : " + value + ". Valeurs autorisées : ACTIVE, DISABLED");
        }
    }
}
