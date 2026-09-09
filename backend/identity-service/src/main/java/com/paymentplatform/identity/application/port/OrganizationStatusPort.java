package com.paymentplatform.identity.application.port;

import java.util.UUID;

/** Port vers Organization Service (vérification statut/type de l'organisation). */
public interface OrganizationStatusPort {

    OrganizationStatus getOrganizationStatus(UUID organizationId);
}