package com.paymentplatform.organization.application.dto;

import com.paymentplatform.organization.domain.model.Product;
import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
    Long id,
    Long supplierId,
    String name,
    String sku,
    String description,
    BigDecimal unitPrice,
    String currency,
    Integer quantity,
    Integer minQuantity,
    Integer reservedQty,
    Long categoryId,
    Long familyId,
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
