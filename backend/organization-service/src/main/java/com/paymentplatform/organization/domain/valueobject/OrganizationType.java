package com.paymentplatform.organization.domain.valueobject;

public enum OrganizationType {
    SUPPLIER,
    SHOP;

    public static OrganizationType from(String value) {
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new com.paymentplatform.shared.domain.exception.DomainException(
                    "Type d'organisation invalide : " + value + ". Valeurs autorisées : SUPPLIER, SHOP");
        }
    }
}
