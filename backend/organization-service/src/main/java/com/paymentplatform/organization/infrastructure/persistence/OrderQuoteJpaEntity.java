package com.paymentplatform.organization.infrastructure.persistence;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_quotes",
        uniqueConstraints = @UniqueConstraint(name = "uq_quote_order_version",
                columnNames = {"order_id", "version"}))
public class OrderQuoteJpaEntity {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "order_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID orderId;

    @Column(name = "order_reference", nullable = false, length = 64)
    private String orderReference;

    @Column(name = "supplier_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID supplierId;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "global_discount", nullable = false, precision = 5, scale = 2)
    private BigDecimal globalDiscount;

    @Column(name = "subtotal", nullable = false, precision = 19, scale = 3)
    private BigDecimal subtotal;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 3)
    private BigDecimal taxAmount;

    @Column(name = "total", nullable = false, precision = 19, scale = 3)
    private BigDecimal total;

    @Column(name = "stamp_amount", nullable = false, precision = 19, scale = 3)
    private BigDecimal stampAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "lines_json", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String linesJson;

    @Column(name = "created_by", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getOrderReference() { return orderReference; }
    public void setOrderReference(String orderReference) { this.orderReference = orderReference; }
    public UUID getSupplierId() { return supplierId; }
    public void setSupplierId(UUID supplierId) { this.supplierId = supplierId; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public BigDecimal getGlobalDiscount() { return globalDiscount; }
    public void setGlobalDiscount(BigDecimal globalDiscount) { this.globalDiscount = globalDiscount; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public BigDecimal getStampAmount() { return stampAmount; }
    public void setStampAmount(BigDecimal stampAmount) { this.stampAmount = stampAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getLinesJson() { return linesJson; }
    public void setLinesJson(String linesJson) { this.linesJson = linesJson; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
