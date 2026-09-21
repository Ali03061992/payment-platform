package com.paymentplatform.shared.domain.security;

import com.paymentplatform.shared.domain.model.RoleCode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.paymentplatform.shared.domain.security.Permissions.*;

/** Catalogue statique rôle → permissions (miroir de la table role_permissions seedée). */
public final class PermissionCatalog {

    private static final Map<RoleCode, Set<String>> BY_ROLE = Map.of(
            RoleCode.SYSTEM_ADMIN, Set.of(ADMIN_MANAGE_ORGANIZATIONS, ADMIN_MANAGE_USERS, ADMIN_VIEW_AUDIT,
                    ADMIN_VIEW_STATS, VIEW_PAYMENTS, VIEW_NOTIFICATIONS),
            RoleCode.SUPPLIER_ADMIN, Set.of(SUPPLIER_MANAGE_AGENTS, SUPPLIER_MANAGE_PAYMENTS,
                    SUPPLIER_MANAGE_PRODUCTS, SUPPLIER_MANAGE_STOCK, VIEW_PAYMENTS,
                    VIEW_NOTIFICATIONS),
            RoleCode.SUPPLIER_AGENT, Set.of(SUPPLIER_MANAGE_PAYMENTS, SUPPLIER_MANAGE_PRODUCTS,
                    SUPPLIER_MANAGE_STOCK, VIEW_PAYMENTS, VIEW_NOTIFICATIONS),
            RoleCode.SHOP_ADMIN, Set.of(SHOP_MANAGE_AGENTS, SHOP_CREATE_PAYMENTS, SHOP_CANCEL_PAYMENTS,
                    VIEW_PAYMENTS, VIEW_NOTIFICATIONS),
            RoleCode.SHOP_AGENT, Set.of(SHOP_CREATE_PAYMENTS, SHOP_CANCEL_PAYMENTS, VIEW_PAYMENTS,
                    VIEW_NOTIFICATIONS));

    private PermissionCatalog() {
    }

    public static Set<String> permissionsFor(RoleCode role) {
        return BY_ROLE.getOrDefault(role, Set.of());
    }

    public static Set<String> permissionsFor(List<String> roleCodes) {
        return roleCodes.stream()
                .map(RoleCode::from)
                .flatMap(role -> permissionsFor(role).stream())
                .collect(Collectors.toSet());
    }
}