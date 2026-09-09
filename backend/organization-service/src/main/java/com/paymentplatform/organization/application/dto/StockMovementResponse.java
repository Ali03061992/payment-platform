package com.paymentplatform.organization.application.dto;

import java.util.UUID;

import com.paymentplatform.organization.domain.model.StockMovement;
import java.time.Instant;

public record StockMovementResponse(
    UUID id,
    UUID productId,
    String productName,
    String type,
    Integer quantity,
    String reference,
    String notes,
    String createdBy,
    Instant createdAt
) {
    public static StockMovementResponse from(StockMovement movement, String productName) {
        return new StockMovementResponse(
            movement.getId(),
            movement.getProductId(),
            productName,
            movement.getType(),
            movement.getQuantity(),
            movement.getReference(),
            movement.getNotes(),
            movement.getCreatedBy(),
            movement.getCreatedAt()
        );
    }
}
