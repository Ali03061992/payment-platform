package com.paymentplatform.identity.application.port;

/** Port vers Organization Service (vérification statut/type de l'organisation). */
public interface OrganizationStatusPort {

    OrganizationStatus getOrganizationStatus(long organizationId);
}