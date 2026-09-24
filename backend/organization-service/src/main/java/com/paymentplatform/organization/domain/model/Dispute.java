package com.paymentplatform.organization.domain.model;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "disputes")
public class Dispute {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "order_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID orderId;

    @Column(name = "shop_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID shopId;

    @Column(name = "supplier_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID supplierId;

    @Column(name = "opened_by", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID openedBy;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false, length = 255)
    private String reason;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "dispute_id", nullable = false)
    @OrderBy("timestamp ASC")
    private List<DisputeMessage> messages = new ArrayList<>();

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        if (this.id == null) { this.id = UUID.randomUUID(); }
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) status = DisputeStatus.OPEN.name();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public static Dispute create(UUID orderId, UUID shopId, UUID supplierId, UUID openedBy, String reason) {
        Dispute dispute = new Dispute();
        dispute.orderId = orderId;
        dispute.shopId = shopId;
        dispute.supplierId = supplierId;
        dispute.openedBy = openedBy;
        dispute.reason = reason;
        dispute.status = DisputeStatus.OPEN.name();
        return dispute;
    }

    public void transitionTo(DisputeStatus newStatus) {
        DisputeStatus current = DisputeStatus.valueOf(this.status);
        current.assertCanTransitionTo(newStatus);
        this.status = newStatus.name();
        this.updatedAt = Instant.now();
    }

    public void addMessage(DisputeMessage message) {
        this.messages.add(message);
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getShopId() { return shopId; }
    public UUID getSupplierId() { return supplierId; }
    public UUID getOpenedBy() { return openedBy; }
    public String getStatus() { return status; }
    public String getReason() { return reason; }
    public List<DisputeMessage> getMessages() { return messages; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setReason(String reason) { this.reason = reason; }
}
