package com.paymentplatform.payment.domain.model;

import com.paymentplatform.payment.domain.valueobject.Money;
import com.paymentplatform.payment.domain.valueobject.PaymentReference;
import com.paymentplatform.payment.domain.valueobject.RejectionReason;
import com.paymentplatform.shared.domain.exception.DomainException;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

public class Payment {

    private final UUID id;
    private final PaymentReference reference;
    private final UUID shopId;
    private final UUID supplierId;
    private final Money money;
    private PaymentStatus status;
    private RejectionReason rejectionReason;
    private final UUID createdBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;
    private final String idempotencyKey;
    private final List<PaymentEvent> events;
    private UUID orderId;
    private LocalDate dueDate;

    private Payment(UUID id, PaymentReference reference, UUID shopId, UUID supplierId,
                    Money money, PaymentStatus status, RejectionReason rejectionReason,
                    UUID createdBy, Instant createdAt, Instant updatedAt, long version,
                    String idempotencyKey, List<PaymentEvent> events, UUID orderId, LocalDate dueDate) {
        this.id = id;
        this.reference = reference;
        this.shopId = shopId;
        this.supplierId = supplierId;
        this.money = money;
        this.status = status;
        this.rejectionReason = rejectionReason;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
        this.idempotencyKey = idempotencyKey;
        this.events = new ArrayList<>(events);
        this.orderId = orderId;
        this.dueDate = dueDate;
    }

public static Payment create(UUID shopId, UUID supplierId, Money money, UUID createdBy) {
        return create(shopId, supplierId, money, createdBy, null, null);
    }

    public static Payment create(UUID shopId, UUID supplierId, Money money, UUID createdBy,
                                 UUID orderId, LocalDate dueDate) {
        return create(shopId, supplierId, money, createdBy, orderId, dueDate, null);
    }

    public static Payment create(UUID shopId, UUID supplierId, Money money, UUID createdBy,
                                 UUID orderId, LocalDate dueDate, String idempotencyKey) {
        if (shopId == null) throw new DomainException("L'ID de la boutique est obligatoire");
        if (supplierId == null) throw new DomainException("L'ID du fournisseur est obligatoire");
        if (shopId.equals(supplierId)) throw new DomainException("La boutique et le fournisseur doivent être différents");
        if (money == null || money.amount() == null) throw new DomainException("Le montant est obligatoire");
        if (money.amount().compareTo(new java.math.BigDecimal("0.01")) < 0)
            throw new DomainException("Le montant doit être supérieur ou égal à 0.01");
        if (money.amount().compareTo(new java.math.BigDecimal("999999.99")) > 0)
            throw new DomainException("Le montant ne doit pas dépasser 999999.99");

        Instant now = Instant.now();
        PaymentReference ref = PaymentReference.generate();
        return new Payment(null, ref, shopId, supplierId, money, PaymentStatus.PENDING,
                null, createdBy, now, now, 0,
                idempotencyKey,
                List.of(PaymentEvent.create(null, AuditActions.PAYMENT_CREATED, createdBy,
                        "{\"reference\":\"" + ref.value() + "\"}")),
                orderId, dueDate);
    }

    public Payment confirm(UUID confirmedBy) {
        assertNotTerminal();
        PaymentStatus.PENDING.assertCanTransitionTo(PaymentStatus.CONFIRMED);

        this.status = PaymentStatus.CONFIRMED;
        this.updatedAt = Instant.now();
        this.events.add(PaymentEvent.create(this.id,
                AuditActions.PAYMENT_CONFIRMED, confirmedBy, null));
        return this;
    }

    public Payment reject(UUID rejectedBy, RejectionReason reason) {
        assertNotTerminal();
        PaymentStatus.PENDING.assertCanTransitionTo(PaymentStatus.REJECTED);

        this.status = PaymentStatus.REJECTED;
        this.rejectionReason = reason;
        this.updatedAt = Instant.now();
        this.events.add(PaymentEvent.create(this.id,
                AuditActions.PAYMENT_REJECTED, rejectedBy,
                "{\"reason\":\"" + reason.value() + "\"}"));
        return this;
    }

    public Payment cancel(UUID cancelledBy) {
        assertNotTerminal();
        PaymentStatus.PENDING.assertCanTransitionTo(PaymentStatus.CANCELLED);

        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = Instant.now();
        this.events.add(PaymentEvent.create(this.id,
                AuditActions.PAYMENT_CANCELLED, cancelledBy, null));
        return this;
    }

    private void assertNotTerminal() {
        if (status.isTerminal()) {
            throw new DomainException("Le paiement " + reference.value()
                    + " est dans un état terminal (" + status + ")");
        }
    }

    public boolean belongsToShop(UUID shopId) {
        return this.shopId.equals(shopId);
    }

    public boolean belongsToSupplier(UUID supplierId) {
        return this.supplierId.equals(supplierId);
    }

    public boolean canBeViewedBy(UUID userId, UUID organizationId) {
        return this.createdBy.equals(userId)
                || (organizationId != null && (this.shopId.equals(organizationId) || this.supplierId.equals(organizationId)));
    }

    public UUID id() { return id; }
    public PaymentReference reference() { return reference; }
    public UUID shopId() { return shopId; }
    public UUID supplierId() { return supplierId; }
    public Money money() { return money; }
    public PaymentStatus status() { return status; }
    public RejectionReason rejectionReason() { return rejectionReason; }
    public UUID createdBy() { return createdBy; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public long version() { return version; }
    public String idempotencyKey() { return idempotencyKey; }
    public List<PaymentEvent> events() { return Collections.unmodifiableList(events); }
    public UUID orderId() { return orderId; }
    public LocalDate dueDate() { return dueDate; }
}
