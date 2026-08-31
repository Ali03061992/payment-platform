package com.paymentplatform.shared.domain.security;

/** Permissions du platform (RBAC par permission, cf. docs/security.md). */
public final class Permissions {

    private Permissions() {
    }

    public static final String ADMIN_MANAGE_ORGANIZATIONS = "ADMIN_MANAGE_ORGANIZATIONS";
    public static final String ADMIN_MANAGE_USERS = "ADMIN_MANAGE_USERS";
    public static final String ADMIN_VIEW_AUDIT = "ADMIN_VIEW_AUDIT";
    public static final String ADMIN_VIEW_STATS = "ADMIN_VIEW_STATS";
    public static final String SUPPLIER_MANAGE_AGENTS = "SUPPLIER_MANAGE_AGENTS";
    public static final String SUPPLIER_MANAGE_PAYMENTS = "SUPPLIER_MANAGE_PAYMENTS";
    public static final String SUPPLIER_MANAGE_PRODUCTS = "SUPPLIER_MANAGE_PRODUCTS";
    public static final String SUPPLIER_MANAGE_STOCK = "SUPPLIER_MANAGE_STOCK";
    public static final String SHOP_MANAGE_AGENTS = "SHOP_MANAGE_AGENTS";
    public static final String SHOP_CREATE_PAYMENTS = "SHOP_CREATE_PAYMENTS";
    public static final String SHOP_CANCEL_PAYMENTS = "SHOP_CANCEL_PAYMENTS";
    public static final String VIEW_PAYMENTS = "VIEW_PAYMENTS";
    public static final String VIEW_NOTIFICATIONS = "VIEW_NOTIFICATIONS";
}