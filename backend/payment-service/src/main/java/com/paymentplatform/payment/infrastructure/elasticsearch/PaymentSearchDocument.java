package com.paymentplatform.payment.infrastructure.elasticsearch;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.math.BigDecimal;
import java.time.Instant;

@Document(indexName = "payments")
public class PaymentSearchDocument {

    @Id
    private Long id;

    @Field(type = FieldType.Keyword)
    private String reference;

    @Field(type = FieldType.Long)
    private long shopId;

    @Field(type = FieldType.Keyword)
    private String shopName;

    @Field(type = FieldType.Long)
    private long supplierId;

    @Field(type = FieldType.Keyword)
    private String supplierName;

    @Field(type = FieldType.Long)
    private long createdBy;

    @Field(type = FieldType.Keyword)
    private String createdByName;

    @Field(type = FieldType.Double)
    private BigDecimal amount;

    @Field(type = FieldType.Keyword)
    private String currency;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Text)
    private String rejectionReason;

    @Field(type = FieldType.Date)
    private Instant createdAt;

    @Field(type = FieldType.Date)
    private Instant updatedAt;

    public PaymentSearchDocument() {}

    public PaymentSearchDocument(Long id, String reference, long shopId, String shopName,
                                  long supplierId, String supplierName, long createdBy, String createdByName,
                                  BigDecimal amount, String currency, String status, String rejectionReason,
                                  Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.reference = reference;
        this.shopId = shopId;
        this.shopName = shopName;
        this.supplierId = supplierId;
        this.supplierName = supplierName;
        this.createdBy = createdBy;
        this.createdByName = createdByName;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getReference() { return reference; }
    public void setReference(String reference) { this.reference = reference; }
    public long getShopId() { return shopId; }
    public void setShopId(long shopId) { this.shopId = shopId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public long getSupplierId() { return supplierId; }
    public void setSupplierId(long supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public long getCreatedBy() { return createdBy; }
    public void setCreatedBy(long createdBy) { this.createdBy = createdBy; }
    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
