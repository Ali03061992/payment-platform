package com.paymentplatform.organization.domain.valueobject;

public enum RelationStatus {
    ACTIVE,
    INACTIVE;

    public static RelationStatus from(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new com.paymentplatform.shared.domain.exception.DomainException(
                    "Statut de relation invalide : " + value);
        }
    }
}
