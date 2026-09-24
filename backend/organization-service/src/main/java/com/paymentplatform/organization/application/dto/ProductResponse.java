package com.paymentplatform.organization.application.dto;

import com.paymentplatform.organization.domain.model.Product;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductResponse(
    UUID id,
    UUID supplierId,
    String name,
    String sku,
    String description,
    String imageUrl,
    BigDecimal unitPrice,
    String currency,
    Integer quantity,
    Integer minQuantity,
    Integer reservedQty,
    UUID categoryId,
    UUID familyId,
    String unit,
    String status,
    Instant createdAt,
    Instant updatedAt
) {
    public static ProductResponse from(Product product) {
        return new ProductResponse(
            product.getId(),
            product.getSupplierId(),
            product.getName(),
            product.getSku(),
            product.getDescription(),
            product.getImageUrl(),
            product.getUnitPrice(),
            product.getCurrency(),
            product.getQuantity(),
            product.getMinQuantity(),
            product.getReservedQty(),
            product.getCategoryId(),
            product.getFamilyId(),
            product.getUnit(),
            product.getStatus(),
            product.getCreatedAt(),
            product.getUpdatedAt()
        );
    }
}
