package com.paymentplatform.identity.infrastructure.test;

import com.paymentplatform.identity.application.port.OrganizationStatus;
import com.paymentplatform.identity.application.port.OrganizationStatusPort;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Implémentation test du port OrganizationStatus.
 * Retourne des valeurs prédéfinies sans appels HTTP.
 */
@Component
@Profile("test")
public class TestOrganizationStatusPort implements OrganizationStatusPort {

    private final Map<UUID, OrganizationStatus> statuses = new HashMap<>(Map.of(
            UUID.fromString("00000000-0000-0000-0000-000000000042"), new OrganizationStatus(UUID.fromString("00000000-0000-0000-0000-000000000042"), "SUPPLIER", "ACTIVE"),
            UUID.fromString("00000000-0000-0000-0000-000000000005"), new OrganizationStatus(UUID.fromString("00000000-0000-0000-0000-000000000005"), "SHOP", "ACTIVE"),
            UUID.fromString("00000000-0000-0000-0000-000000000007"), new OrganizationStatus(UUID.fromString("00000000-0000-0000-0000-000000000007"), "SHOP", "ACTIVE"),
            UUID.fromString("00000000-0000-0000-0000-000000000100"), new OrganizationStatus(UUID.fromString("00000000-0000-0000-0000-000000000100"), "SUPPLIER", "ACTIVE")
    ));

    private OrganizationStatus defaultStatus = new OrganizationStatus(UUID.fromString("00000000-0000-0000-0000-000000000000"), "SUPPLIER", "ACTIVE");

    @Override
    public OrganizationStatus getOrganizationStatus(UUID organizationId) {
        return statuses.getOrDefault(organizationId, defaultStatus);
    }

    public void setStatus(UUID id, String type, String status) {
        statuses.put(id, new OrganizationStatus(id, type, status));
    }
}
