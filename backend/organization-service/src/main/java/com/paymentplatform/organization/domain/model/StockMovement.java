package com.paymentplatform.organization.domain.model;

import java.util.UUID;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "product_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID productId;

    @Column(name = "supplier_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID supplierId;

    @Column(nullable = false, length = 20)
    private String type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(length = 100)
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (this.id == null) { this.id = java.util.UUID.randomUUID(); }

        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
