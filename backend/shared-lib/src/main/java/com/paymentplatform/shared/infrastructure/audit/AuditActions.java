package com.paymentplatform.shared.infrastructure.audit;

/** Actions d'audit standardisées (cf. docs/business-rules.md §8). */
public final class AuditActions {

    private AuditActions() {
    }

    public static final String PAYMENT_CREATED = "PAYMENT_CREATED";
    public static final String PAYMENT_CONFIRMED = "PAYMENT_CONFIRMED";
    public static final String PAYMENT_REJECTED = "PAYMENT_REJECTED";
    public static final String PAYMENT_CANCELLED = "PAYMENT_CANCELLED";

    public static final String SUPPLIER_CREATED = "SUPPLIER_CREATED";
    public static final String SUPPLIER_ENABLED = "SUPPLIER_ENABLED";
    public static final String SUPPLIER_DISABLED = "SUPPLIER_DISABLED";

    public static final String SHOP_CREATED = "SHOP_CREATED";
    public static final String SHOP_ENABLED = "SHOP_ENABLED";
    public static final String SHOP_DISABLED = "SHOP_DISABLED";

    public static final String USER_CREATED = "USER_CREATED";
    public static final String USER_ENABLED = "USER_ENABLED";
    public static final String USER_DISABLED = "USER_DISABLED";

    public static final String USER_LOGIN = "USER_LOGIN";
    public static final String USER_LOGOUT = "USER_LOGOUT";
    public static final String USER_TOKEN_REFRESH = "USER_TOKEN_REFRESH";
    public static final String USER_PASSWORD_CHANGE = "USER_PASSWORD_CHANGE";
}