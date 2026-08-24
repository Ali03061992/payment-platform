package com.paymentplatform.organization.domain.model;

import jakarta.persistence.*;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false)
    private Long supplierId;

    @Column(name = "shop_id", nullable = false)
    private Long shopId;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    @Column(name = "order_id")
    private Long orderId;

    @Column(name = "payment_id")
    private Long paymentId;

    @Column(length = 100)
    private String reference;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BalanceEntry() {
    }

    private BalanceEntry(Long supplierId, Long shopId, String type, BigDecimal amount,
                         BigDecimal balanceAfter, Long orderId, Long paymentId,
                         String reference, String reason, Long createdBy) {
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

    public static BalanceEntry create(Long supplierId, Long shopId, String type, BigDecimal amount,
                                      BigDecimal balanceAfter, Long orderId, Long paymentId,
                                      String reference, String reason, Long createdBy) {
        return new BalanceEntry(supplierId, shopId, type, amount, balanceAfter,
                orderId, paymentId, reference, reason, createdBy);
    }

    public Long getId() { return id; }
    public Long getSupplierId() { return supplierId; }
    public Long getShopId() { return shopId; }
    public String getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public BigDecimal getBalanceAfter() { return balanceAfter; }
    public Long getOrderId() { return orderId; }
    public Long getPaymentId() { return paymentId; }
    public String getReference() { return reference; }
    public String getReason() { return reason; }
    public Long getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
