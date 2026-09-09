package com.paymentplatform.payment.infrastructure.persistence;

import java.util.UUID;
import jakarta.persistence.PrePersist;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class PaymentJpaEntity {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    @Column(name = "shop_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID shopId;

    @Column(name = "supplier_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID supplierId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "created_by", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public UUID getShopId() { return shopId; }
    public void setShopId(UUID shopId) { this.shopId = shopId; }
    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) { this.id = java.util.UUID.randomUUID(); }
    }
}
