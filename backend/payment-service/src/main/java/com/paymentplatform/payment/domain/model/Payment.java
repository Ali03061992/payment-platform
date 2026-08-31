package com.paymentplatform.payment.domain.model;

import com.paymentplatform.shared.domain.exception.DomainException;
import com.paymentplatform.shared.infrastructure.audit.AuditActions;
import com.paymentplatform.payment.domain.valueobject.Money;
import com.paymentplatform.payment.domain.valueobject.PaymentReference;
import com.paymentplatform.payment.domain.valueobject.RejectionReason;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Payment {

    private final Long id;
    private final PaymentReference reference;
    private final long shopId;
    private final long supplierId;
    private final Money money;
    private PaymentStatus status;
    private RejectionReason rejectionReason;
    private final long createdBy;
    private final Instant createdAt;
    private Instant updatedAt;
    private long version;
    private final List<PaymentEvent> events;

    private Payment(Long id, PaymentReference reference, long shopId, long supplierId,
                    Money money, PaymentStatus status, RejectionReason rejectionReason,
                    long createdBy, Instant createdAt, Instant updatedAt, long version,
                    List<PaymentEvent> events) {
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
        this.events = new ArrayList<>(events);
    }

    public static Payment create(long shopId, long supplierId, Money money, long createdBy) {
        if (shopId <= 0) throw new DomainException("L'ID de la boutique est obligatoire");
        if (supplierId <= 0) throw new DomainException("L'ID du fournisseur est obligatoire");
        if (shopId == supplierId) throw new DomainException("La boutique et le fournisseur doivent être différents");
        if (money == null || money.amount() == null) throw new DomainException("Le montant est obligatoire");
        if (money.amount().compareTo(java.math.BigDecimal.ZERO) <= 0)
            throw new DomainException("Le montant doit être supérieur à 0");

        Instant now = Instant.now();
        PaymentReference ref = PaymentReference.generate();
        return new Payment(null, ref, shopId, supplierId, money, PaymentStatus.PENDING,
                null, createdBy, now, now, 0,
                List.of(PaymentEvent.create(0, AuditActions.PAYMENT_CREATED, createdBy,
                        "{\"reference\":\"" + ref.value() + "\"}")));
    }

    public Payment confirm(long confirmedBy) {
        assertNotTerminal();
        PaymentStatus.PENDING.assertCanTransitionTo(PaymentStatus.CONFIRMED);

        this.status = PaymentStatus.CONFIRMED;
        this.updatedAt = Instant.now();
        this.events.add(PaymentEvent.create(this.id != null ? this.id : 0,
                AuditActions.PAYMENT_CONFIRMED, confirmedBy, null));
        return this;
    }

    public Payment reject(long rejectedBy, RejectionReason reason) {
        assertNotTerminal();
        PaymentStatus.PENDING.assertCanTransitionTo(PaymentStatus.REJECTED);

        this.status = PaymentStatus.REJECTED;
        this.rejectionReason = reason;
        this.updatedAt = Instant.now();
        this.events.add(PaymentEvent.create(this.id != null ? this.id : 0,
                AuditActions.PAYMENT_REJECTED, rejectedBy,
                "{\"reason\":\"" + reason.value() + "\"}"));
        return this;
    }

    public Payment cancel(long cancelledBy) {
        assertNotTerminal();
        PaymentStatus.PENDING.assertCanTransitionTo(PaymentStatus.CANCELLED);

        this.status = PaymentStatus.CANCELLED;
        this.updatedAt = Instant.now();
        this.events.add(PaymentEvent.create(this.id != null ? this.id : 0,
                AuditActions.PAYMENT_CANCELLED, cancelledBy, null));
        return this;
    }

    private void assertNotTerminal() {
        if (status.isTerminal()) {
            throw new DomainException("Le paiement " + reference.value()
                    + " est dans un état terminal (" + status + ")");
        }
    }

    public boolean belongsToShop(long shopId) {
        return this.shopId == shopId;
    }

    public boolean belongsToSupplier(long supplierId) {
        return this.supplierId == supplierId;
    }

    public boolean canBeViewedBy(long userId, Long organizationId) {
        return this.createdBy == userId
                || (organizationId != null && (this.shopId == organizationId || this.supplierId == organizationId));
    }

    public Long id() { return id; }
    public PaymentReference reference() { return reference; }
    public long shopId() { return shopId; }
    public long supplierId() { return supplierId; }
    public Money money() { return money; }
    public PaymentStatus status() { return status; }
    public RejectionReason rejectionReason() { return rejectionReason; }
    public long createdBy() { return createdBy; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
    public long version() { return version; }
    public List<PaymentEvent> events() { return Collections.unmodifiableList(events); }
}
