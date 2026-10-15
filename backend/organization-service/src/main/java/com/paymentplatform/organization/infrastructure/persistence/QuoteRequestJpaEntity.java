package com.paymentplatform.organization.infrastructure.persistence;

import com.paymentplatform.organization.domain.model.QuoteRequestStatus;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "quote_requests")
public class QuoteRequestJpaEntity {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "reference", nullable = false, unique = true, length = 64)
    private String reference;

    @Column(name = "supplier_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID supplierId;

    @Column(name = "supplier_name", length = 100)
    private String supplierName;

    @Column(name = "shop_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID shopId;

    @Column(name = "shop_name", length = 100)
    private String shopName;

    @Column(length = 1000)
    private String notes;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private QuoteRequestStatus status;

    @Column(name = "lines_json", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String linesJson;

    @Column(name = "created_by", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public UUID getShopId() { return shopId; }
    public void setShopId(UUID shopId) { this.shopId = shopId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public QuoteRequestStatus getStatus() { return status; }
    public void setStatus(QuoteRequestStatus status) { this.status = status; }
    public String getLinesJson() { return linesJson; }
    public void setLinesJson(String linesJson) { this.linesJson = linesJson; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
