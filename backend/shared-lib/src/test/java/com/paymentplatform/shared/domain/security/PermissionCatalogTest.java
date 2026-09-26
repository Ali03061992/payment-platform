package com.paymentplatform.shared.domain.security;

import com.paymentplatform.shared.domain.model.RoleCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Tests de PermissionCatalogTest.
 * Perimetre : regles metier de PermissionCatalog (invariants, transitions).
 * Moyens : JUnit pur (AssertJ).
 */

class PermissionCatalogTest {

    @Test
    void permissionsFor_systemAdmin() {
        Set<String> perms = PermissionCatalog.permissionsFor(RoleCode.SYSTEM_ADMIN);
        assertThat(perms).contains("ADMIN_MANAGE_ORGANIZATIONS", "ADMIN_MANAGE_USERS",
                "ADMIN_VIEW_AUDIT", "ADMIN_VIEW_STATS", "VIEW_PAYMENTS", "VIEW_NOTIFICATIONS");
    }

    @Test
    void permissionsFor_supplierAdmin() {
        Set<String> perms = PermissionCatalog.permissionsFor(RoleCode.SUPPLIER_ADMIN);
        assertThat(perms).contains("SUPPLIER_MANAGE_AGENTS", "SUPPLIER_MANAGE_PAYMENTS",
                "SUPPLIER_MANAGE_PRODUCTS", "SUPPLIER_MANAGE_STOCK", "VIEW_PAYMENTS");
    }

    @Test
    void permissionsFor_supplierAgent() {
        Set<String> perms = PermissionCatalog.permissionsFor(RoleCode.SUPPLIER_AGENT);
        assertThat(perms).contains("SUPPLIER_MANAGE_PAYMENTS", "SUPPLIER_MANAGE_PRODUCTS",
                "SUPPLIER_MANAGE_STOCK", "VIEW_PAYMENTS");
        assertThat(perms).doesNotContain("SUPPLIER_MANAGE_AGENTS");
    }

    @Test
    void permissionsFor_shopAdmin() {
        Set<String> perms = PermissionCatalog.permissionsFor(RoleCode.SHOP_ADMIN);
        assertThat(perms).contains("SHOP_MANAGE_AGENTS", "SHOP_CREATE_PAYMENTS",
                "SHOP_CANCEL_PAYMENTS", "VIEW_PAYMENTS");
    }

    @Test
    void permissionsFor_shopAgent() {
        Set<String> perms = PermissionCatalog.permissionsFor(RoleCode.SHOP_AGENT);
        assertThat(perms).contains("SHOP_CREATE_PAYMENTS", "SHOP_CANCEL_PAYMENTS", "VIEW_PAYMENTS");
        assertThat(perms).doesNotContain("SHOP_MANAGE_AGENTS");
    }

    @Test
    void permissionsFor_roleCodesList() {
        var result = PermissionCatalog.permissionsFor(List.of("SYSTEM_ADMIN", "SHOP_AGENT"));
        assertThat(result).contains("ADMIN_MANAGE_ORGANIZATIONS", "SHOP_CREATE_PAYMENTS");
    }

    @Test
    void permissionsFor_emptyList() {
        var result = PermissionCatalog.permissionsFor(List.of());
        assertThat(result).isEmpty();
    }

    @Test
    void allRoles_havePermissions() {
        for (RoleCode role : RoleCode.values()) {
            Set<String> perms = PermissionCatalog.permissionsFor(role);
            assertThat(perms).isNotEmpty();
        }
    }
}
