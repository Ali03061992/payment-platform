package com.paymentplatform.organization.domain.model;

import java.util.UUID;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "balance_ledger")
public class BalanceEntry {

    public static final String ORDER_CREDIT = "ORDER_CREDIT";
    public static final String PAYMENT_DEBIT = "PAYMENT_DEBIT";
    public static final String ADJUSTMENT = "ADJUSTMENT";
    public static final String REFUND = "REFUND";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @UuidGenerator(style = UuidGenerator.Style.RANDOM)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "supplier_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID supplierId;

    @Column(name = "shop_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID shopId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "order_id", columnDefinition = "VARCHAR(36)")
    private UUID orderId;

    @Column(name = "payment_id", columnDefinition = "VARCHAR(36)")
    private UUID paymentId;

    @Column(length = 100)
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_by", columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BalanceEntry() {
    }

    private BalanceEntry(UUID supplierId, UUID shopId, String type, BigDecimal amount,
                         BigDecimal balanceAfter, UUID orderId, UUID paymentId,
                         String reference, String reason, UUID createdBy) {
        this.supplierId = supplierId;
        this.shopId = shopId;
        this.type = type;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.orderId = orderId;
        this.paymentId = paymentId;
        this.reference = reference;
        this.reason = reason;
        this.createdBy = createdBy;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
    }

    public static BalanceEntry create(UUID supplierId, UUID shopId, String type, BigDecimal amount,
                                      BigDecimal balanceAfter, UUID orderId, UUID paymentId,
                                      String reference, String reason, UUID createdBy) {
        return new BalanceEntry(supplierId, shopId, type, amount, balanceAfter,
                orderId, paymentId, reference, reason, createdBy);
    }

    public UUID getId() { return id; }
    public UUID getSupplierId() { return supplierId; }
    public UUID getShopId() { return shopId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public UUID getOrderId() { return orderId; }
    public UUID getPaymentId() { return paymentId; }
    public String getReference() { return reference; }
    public String getReason() { return reason; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
