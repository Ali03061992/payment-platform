package com.paymentplatform.organization.application.dto;

import com.paymentplatform.organization.domain.model.SupplierShopRelation;

import java.time.Instant;

public record RelationResponse(
        long id,
        long supplierId,
        long shopId,
        String status,
        Instant createdAt
) {
    public static RelationResponse from(SupplierShopRelation relation) {
        return new RelationResponse(
                relation.id(),
                relation.supplierId().value(),
                relation.shopId().value(),
                relation.status().name(),
                relation.createdAt());
    }
}
