package com.paymentplatform.shared.domain.model;

/** Rôles du platform (RBAC). */
public enum RoleCode {
    SYSTEM_ADMIN,
    SUPPLIER_ADMIN,
    SUPPLIER_AGENT,
    SHOP_ADMIN,
    SHOP_AGENT;

    public static RoleCode from(String value) {
        for (RoleCode role : values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Rôle inconnu : " + value);
    }
}