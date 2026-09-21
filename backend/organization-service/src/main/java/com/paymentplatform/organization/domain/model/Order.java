package com.paymentplatform.organization.domain.model;

import com.paymentplatform.shared.domain.exception.ConflictException;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "orders")
public class Order {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, unique = true, length = 64)
    private String reference;

    @Column(name = "supplier_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID supplierId;

    @Column(name = "shop_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID shopId;

    @Column(name = "created_by", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @Column(name = "created_by_role", nullable = false, length = 30)
    private String createdByRole;

    @Column(nullable = false, length = 10)
    private String source;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal subtotal;

    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @Column(name = "tax_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal taxAmount;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal total;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "delivery_agent_id", columnDefinition = "VARCHAR(36)")
    private UUID deliveryAgentId;

    @Column(name = "received_by", columnDefinition = "VARCHAR(36)")
    private UUID receivedBy;

    @Column(name = "received_at")
    private Instant receivedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "planned_delivery_date")
    private LocalDate plannedDeliveryDate;

    @Column(name = "confirmed_delivery_date")
    private LocalDate confirmedDeliveryDate;

    @Column(name = "asap_payment", nullable = false)
    private boolean asapPayment;

    @Column(name = "delivery_rejection_reason", columnDefinition = "TEXT")
    private String deliveryRejectionReason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (this.id == null) { this.id = java.util.UUID.randomUUID(); }

        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) status = OrderStatus.DRAFT.name();
        if (subtotal == null) subtotal = BigDecimal.ZERO;
        if (taxRate == null) taxRate = BigDecimal.ZERO;
        if (taxAmount == null) taxAmount = BigDecimal.ZERO;
        if (total == null) total = BigDecimal.ZERO;
        if (currency == null) currency = "TND";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public static Order create(UUID supplierId, UUID shopId, UUID createdBy,
                               String createdByRole, String source, boolean asapPayment, String currency) {
        if (supplierId == null || shopId == null) {
            throw new ConflictException("Le fournisseur et la boutique sont requis");
        }
        if (supplierId.equals(shopId)) {
            throw new ConflictException("Le fournisseur et la boutique doivent être différents");
        }
        Order order = new Order();
        order.reference = "ORD-" + java.util.UUID.randomUUID().toString().substring(0, 8).toUpperCase()
                + "-" + System.currentTimeMillis();
        order.supplierId = supplierId;
        order.shopId = shopId;
        order.createdBy = createdBy;
        order.createdByRole = createdByRole;
        order.source = source;
        order.asapPayment = asapPayment;
        order.currency = currency != null ? currency : "TND";
        order.status = OrderStatus.DRAFT.name();
        order.subtotal = BigDecimal.ZERO;
        order.taxRate = BigDecimal.ZERO;
        order.taxAmount = BigDecimal.ZERO;
        order.total = BigDecimal.ZERO;
        return order;
    }

    private void transitionTo(OrderStatus newStatus) {
        OrderStatus current = OrderStatus.valueOf(this.status);
        current.assertCanTransitionTo(newStatus);
        this.status = newStatus.name();
        this.updatedAt = Instant.now();
    }

    public void confirm() {
        transitionTo(OrderStatus.CONFIRMED);
    }

    public void prepare() {
        transitionTo(OrderStatus.PREPARING);
    }

    public void readyForDelivery() {
        transitionTo(OrderStatus.READY_FOR_DELIVERY);
    }

    public void assignDeliveryAgent(UUID agentId) {
        if (agentId == null) {
            throw new ConflictException("L'ID de l'agent de livraison est requis");
        }
        this.deliveryAgentId = agentId;
        if (this.plannedDeliveryDate == null) {
            this.plannedDeliveryDate = LocalDate.now().plusDays(1);
        }
        this.updatedAt = Instant.now();
    }

    public void confirmDelivery(LocalDate confirmedDate) {
        if (confirmedDate == null) {
            throw new ConflictException("La date de livraison confirmée est requise");
        }
        this.confirmedDeliveryDate = confirmedDate;
        transitionTo(OrderStatus.IN_DELIVERY);
    }

    public void deliver(UUID receivedBy) {
        transitionTo(OrderStatus.DELIVERED);
        this.receivedBy = receivedBy;
        this.receivedAt = Instant.now();
        this.deliveredAt = Instant.now();
    }

    public void accept() {
        transitionTo(OrderStatus.ACCEPTED);
    }

    public void acceptDelivery() {
        transitionTo(OrderStatus.DELIVERY_ACCEPTED);
    }

    public void cancel() {
        transitionTo(OrderStatus.CANCELLED);
    }

    public void reject() {
        transitionTo(OrderStatus.REJECTED);
    }

    public void deliveryReject(String reason) {
        this.deliveryRejectionReason = reason;
        transitionTo(OrderStatus.DELIVERY_REJECTED);
    }

    public void recalculateTotals() {
        // Subtotal is expected to be set by the caller based on order items
        this.taxAmount = this.subtotal.multiply(this.taxRate).divide(new BigDecimal("100"), 4, BigDecimal.ROUND_HALF_UP);
        this.total = this.subtotal.add(this.taxAmount);
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public UUID getSupplierId() { return supplierId; }
    public UUID getShopId() { return shopId; }
    public UUID getCreatedBy() { return createdBy; }
    public String getCreatedByRole() { return createdByRole; }
    public String getSource() { return source; }
    public String getStatus() { return status; }
    public BigDecimal getSubtotal() { return subtotal; }
    public BigDecimal getTaxRate() { return taxRate; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public BigDecimal getTotal() { return total; }
    public String getCurrency() { return currency; }
    public UUID getDeliveryAgentId() { return deliveryAgentId; }
    public UUID getReceivedBy() { return receivedBy; }
    public Instant getReceivedAt() { return receivedAt; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public boolean isAsapPayment() { return asapPayment; }
    public String getDeliveryRejectionReason() { return deliveryRejectionReason; }
    public LocalDate getPlannedDeliveryDate() { return plannedDeliveryDate; }
    public LocalDate getConfirmedDeliveryDate() { return confirmedDeliveryDate; }
    public String getNotes() { return notes; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public void setPlannedDeliveryDate(LocalDate plannedDeliveryDate) { this.plannedDeliveryDate = plannedDeliveryDate; }
    public void setNotes(String notes) { this.notes = notes; }
}
