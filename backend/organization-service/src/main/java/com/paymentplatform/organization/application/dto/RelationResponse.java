package com.paymentplatform.organization.application.dto;

import com.paymentplatform.organization.domain.model.SupplierShopRelation;

import java.time.Instant;
import java.util.UUID;

public record RelationResponse(
        UUID id,
        UUID supplierId,
        UUID shopId,
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
