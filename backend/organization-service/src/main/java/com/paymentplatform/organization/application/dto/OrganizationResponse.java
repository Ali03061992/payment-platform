package com.paymentplatform.organization.application.dto;

import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.model.SupplierShopRelation;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrganizationResponse(
        UUID id,
        String name,
        String type,
        String status,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<RelationResponse> relations,
        String matriculeFiscal,
        String patente,
        String tejIdentifier,
        String defaultWithholdingNature,
        String address,
        String fiscalRegime,
        String suspensionAttestationNumber,
        java.time.LocalDate suspensionAttestationDate,
        java.time.LocalDate suspensionExpiryDate,
        boolean teifEnabled,
        boolean tejEnabled
) {
    public static OrganizationResponse from(Organization org) {
        return from(org, List.of());
    }

    public static OrganizationResponse from(Organization org, List<SupplierShopRelation> relations) {
        return new OrganizationResponse(
                org.id().value(),
                org.name().value(),
                org.type().name(),
                org.status().name(),
                org.version(),
                org.createdAt(),
                org.updatedAt(),
                relations.stream().map(RelationResponse::from).toList(),
                org.matriculeFiscal(),
                org.patente(),
                org.tejIdentifier(),
                org.defaultWithholdingNature(),
                org.address(),
                org.fiscalRegime().name(),
                org.suspensionAttestationNumber(),
                org.suspensionAttestationDate(),
                org.suspensionExpiryDate(),
                org.teifEnabled(),
                org.tejEnabled());
    }
}
