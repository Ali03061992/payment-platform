package com.paymentplatform.organization.application.dto;

import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.model.SupplierShopRelation;

import java.time.Instant;
import java.util.List;

public record OrganizationResponse(
        long id,
        String name,
        String type,
        String status,
        long version,
        Instant createdAt,
        Instant updatedAt,
        List<RelationResponse> relations
) {
    public static OrganizationResponse from(Organization org) {
        return new OrganizationResponse(
                org.id().value(),
                org.name().value(),
                org.type().name(),
                org.status().name(),
                org.version(),
                org.createdAt(),
                org.updatedAt(),
                List.of());
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
                relations.stream().map(RelationResponse::from).toList());
    }
}
