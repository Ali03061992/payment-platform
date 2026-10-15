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

    /** Total FODEC de la commande (1% du HT net des lignes concernées). */
    @Column(name = "fodec_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal fodecTotal;

    /** Droit de timbre fiscal du pied de facture (1,000 DT ; 0 à l'export). */
    @Column(name = "stamp_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal stampAmount;

    /** Ventilation TVA par taux au format « taux|base|montant;... » (FiscalRules). */
    @Column(name = "vat_breakdown", columnDefinition = "TEXT")
    private String vatBreakdown;

    /** Mention légale d'exonération portée sur la facture (art. 11), le cas échéant. */
    @Column(name = "fiscal_mention", columnDefinition = "TEXT")
    private String fiscalMention;

    /** Motif d'exonération (référence du texte / n° attestation) pour les lignes à 0%. */
    @Column(name = "exemption_reason", length = 255)
    private String exemptionReason;

    /** Régime fiscal de l'acheteur au moment de la commande (FiscalRegime). */
    @Column(name = "buyer_regime", length = 30)
    private String buyerRegime;

    /**
     * Référence électronique externe de la facture (plateforme TTN/El Fatoora pour
     * les e-factures, ou TEJ) : affichée avec QR en haut à droite, comme les
     * outils tunisiens (référence unique + QR de validation).
     */
    @Column(name = "external_reference", length = 64)
    private String externalReference;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_terms", length = 20)
    private PaymentTerms paymentTerms;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "delivery_rejection_reason", columnDefinition = "TEXT")
    private String deliveryRejectionReason;

    @Column(name = "return_reason", columnDefinition = "TEXT")
    private String returnReason;

    @Column(name = "estimated_arrival")
    private Instant estimatedArrival;

    @Column(name = "last_latitude", precision = 9, scale = 6)
    private BigDecimal lastLatitude;

    @Column(name = "last_longitude", precision = 9, scale = 6)
    private BigDecimal lastLongitude;

    @Column(name = "last_location_update")
    private Instant lastLocationUpdate;

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
        if (taxRate == null) taxRate = new BigDecimal("19");
        if (taxAmount == null) taxAmount = BigDecimal.ZERO;
        if (fodecTotal == null) fodecTotal = BigDecimal.ZERO;
        if (stampAmount == null) stampAmount = FiscalRules.STAMP_AMOUNT;
        if (buyerRegime == null) buyerRegime = FiscalRegime.ASSUJETTI.name();
        if (total == null) total = BigDecimal.ZERO;
        if (currency == null) currency = "TND";
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public static Order create(UUID supplierId, UUID shopId, UUID createdBy,
                               String createdByRole, String source, boolean asapPayment,
                               String currency, PaymentTerms paymentTerms) {
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
        order.paymentTerms = paymentTerms != null ? paymentTerms : PaymentTerms.IMMEDIATE;
        order.dueDate = order.paymentTerms.computeDueDate(LocalDate.now());
        order.status = OrderStatus.DRAFT.name();
        order.subtotal = BigDecimal.ZERO;
        order.taxRate = new BigDecimal("19");
        order.taxAmount = BigDecimal.ZERO;
        order.fodecTotal = BigDecimal.ZERO;
        order.stampAmount = FiscalRules.STAMP_AMOUNT;
        order.buyerRegime = FiscalRegime.ASSUJETTI.name();
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
        this.estimatedArrival = confirmedDate.atTime(java.time.LocalTime.of(18, 0))
                .atZone(java.time.ZoneId.systemDefault()).toInstant();
        transitionTo(OrderStatus.IN_DELIVERY);
    }

    public void deliver(UUID receivedBy) {
        if (receivedBy == null) {
            throw new ConflictException("Le destinataire de la livraison est requis");
        }
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

    public void updateLocation(BigDecimal latitude, BigDecimal longitude) {
        this.lastLatitude = latitude;
        this.lastLongitude = longitude;
        this.lastLocationUpdate = Instant.now();
        this.updatedAt = Instant.now();
    }

    /**
     * Recalcule les totaux selon l'ordre fiscal tunisien légal : remises lignes,
     * FODEC dans la base TVA, TVA ventilée par taux de ligne.
     * Met à jour chaque ligne (fodecAmount, vatBase, vatAmount) et les totaux.
     * {@code total} = TTC hors timbre ; net à payer = {@link #getNetPayable()}.
     */
    public void recalculateTotals(java.util.List<OrderItem> items) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal fodecTotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        java.util.Map<BigDecimal, FiscalRules.VatLine> byRate = new java.util.LinkedHashMap<>();

        if (items != null) {
            for (OrderItem item : items) {
                FiscalRules.LineResult r = FiscalRules.computeLine(
                        item.getUnitPrice(), item.getQuantity(), item.getDiscount(),
                        item.getVatRate(), item.isFodecApplicable(),
                        item.isPriceTtc());
                item.setFodecAmount(r.fodec());
                item.setVatBase(r.base());
                item.setVatAmount(r.vat());
                item.setHtUnitPrice(r.htUnitPrice());
                subtotal = subtotal.add(r.netHt());
                fodecTotal = fodecTotal.add(r.fodec());
                taxAmount = taxAmount.add(r.vat());
                total = total.add(r.ttc());
                byRate = FiscalRules.accumulate(byRate, item.getVatRate(), r.base(), r.vat());
            }
        }

        this.subtotal = subtotal.setScale(4, java.math.RoundingMode.HALF_UP);
        this.fodecTotal = fodecTotal.setScale(4, java.math.RoundingMode.HALF_UP);
        this.taxAmount = taxAmount.setScale(4, java.math.RoundingMode.HALF_UP);
        this.total = total.setScale(4, java.math.RoundingMode.HALF_UP);
        this.vatBreakdown = FiscalRules.buildVatBreakdown(byRate);
        // Compatibilité historique : taxRate = taux maximal des lignes (lecteurs legacy).
        this.taxRate = byRate.keySet().stream()
                .max(java.util.Comparator.naturalOrder())
                .orElse(new BigDecimal("19"));
        this.updatedAt = Instant.now();
    }

    /** Net à payer : TTC + timbre fiscal (retenue à la source P1 non déduite ici). */
    public BigDecimal getNetPayable() {
        BigDecimal stamp = stampAmount != null ? stampAmount : BigDecimal.ZERO;
        BigDecimal ttc = total != null ? total : BigDecimal.ZERO;
        return ttc.add(stamp).setScale(4, java.math.RoundingMode.HALF_UP);
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
    public void setAsapPayment(boolean asapPayment) { this.asapPayment = asapPayment; }
    public PaymentTerms getPaymentTerms() { return paymentTerms; }
    public void setPaymentTerms(PaymentTerms paymentTerms) { this.paymentTerms = paymentTerms; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public String getDeliveryRejectionReason() { return deliveryRejectionReason; }
    public String getReturnReason() { return returnReason; }
    public LocalDate getPlannedDeliveryDate() { return plannedDeliveryDate; }
    public LocalDate getConfirmedDeliveryDate() { return confirmedDeliveryDate; }
    public String getNotes() { return notes; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
    /**
     * @deprecated Taux global historique. La TVA est désormais portée par chaque
     * ligne (OrderItem.vatRate). Conservé pour compatibilité de lecture.
     */
    @Deprecated
    public void setTaxRate(BigDecimal taxRate) { this.taxRate = taxRate; }
    public BigDecimal getFodecTotal() { return fodecTotal; }
    public BigDecimal getStampAmount() { return stampAmount; }
    public void setStampAmount(BigDecimal stampAmount) { this.stampAmount = stampAmount; }
    public String getVatBreakdown() { return vatBreakdown; }
    public String getFiscalMention() { return fiscalMention; }
    public void setFiscalMention(String fiscalMention) { this.fiscalMention = fiscalMention; }
    public String getExemptionReason() { return exemptionReason; }
    public void setExemptionReason(String exemptionReason) { this.exemptionReason = exemptionReason; }
    public String getBuyerRegime() { return buyerRegime; }
    public void setBuyerRegime(String buyerRegime) { this.buyerRegime = buyerRegime; }
    public String getExternalReference() { return externalReference; }
    public void setExternalReference(String externalReference) { this.externalReference = externalReference; }
    public void setPlannedDeliveryDate(LocalDate plannedDeliveryDate) { this.plannedDeliveryDate = plannedDeliveryDate; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setReturnReason(String returnReason) { this.returnReason = returnReason; }
    public Instant getEstimatedArrival() { return estimatedArrival; }
    public void setEstimatedArrival(Instant estimatedArrival) { this.estimatedArrival = estimatedArrival; }
    public BigDecimal getLastLatitude() { return lastLatitude; }
    public BigDecimal getLastLongitude() { return lastLongitude; }
    public Instant getLastLocationUpdate() { return lastLocationUpdate; }
}
