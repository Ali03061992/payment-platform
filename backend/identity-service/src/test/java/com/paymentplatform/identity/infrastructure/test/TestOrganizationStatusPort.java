package com.paymentplatform.identity.infrastructure.test;

import com.paymentplatform.identity.application.port.OrganizationStatus;
import com.paymentplatform.identity.application.port.OrganizationStatusPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * Implémentation test du port OrganizationStatus.
 * Retourne des valeurs prédéfinies sans appels HTTP.
 */
@Component
@Profile("test")
public class TestOrganizationStatusPort implements OrganizationStatusPort {

    private final Map<Long, OrganizationStatus> statuses = new HashMap<>(Map.of(
            42L, new OrganizationStatus(42, "SUPPLIER", "ACTIVE"),
            5L, new OrganizationStatus(5, "SHOP", "ACTIVE"),
            7L, new OrganizationStatus(7, "SHOP", "ACTIVE"),
            100L, new OrganizationStatus(100, "SUPPLIER", "ACTIVE")
    ));

    private OrganizationStatus defaultStatus = new OrganizationStatus(0, "SUPPLIER", "ACTIVE");

    @Override
    public OrganizationStatus getOrganizationStatus(long organizationId) {
        return statuses.getOrDefault(organizationId, defaultStatus);
    }

    public void setStatus(long id, String type, String status) {
        statuses.put(id, new OrganizationStatus(id, type, status));
    }
}
