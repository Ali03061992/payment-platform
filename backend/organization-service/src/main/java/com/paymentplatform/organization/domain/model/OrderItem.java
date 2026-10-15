package com.paymentplatform.organization.domain.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "order_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID orderId;

    @Column(name = "product_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID productId;

    @Column(name = "product_ref", length = 50)
    private String productRef;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal discount;

    @Column(name = "line_total", nullable = false, precision = 19, scale = 4)
    private BigDecimal lineTotal;

    /**
     * Snapshot fiscal au moment de la commande (le taux produit peut changer après).
     * vatRate : taux appliqué à la ligne ; fodecAmount : FODEC de la ligne (HT net + remise
     * globale) ; vatBase : base TVA = net + FODEC ; vatAmount : TVA de la ligne.
     */
    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal vatRate;

    @Column(name = "fodec_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal fodecAmount;

    @Column(name = "vat_base", nullable = false, precision = 19, scale = 4)
    private BigDecimal vatBase;

    @Column(name = "vat_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal vatAmount;

    /** FODEC applicable snapshoté depuis la fiche produit (décision ligne par ligne). */
    @Column(name = "fodec_applicable", nullable = false)
    private boolean fodecApplicable;

    /** Prix TTC snapshoté (TVA incluse, déduite au lieu d'être ajoutée). */
    @Column(name = "price_ttc", nullable = false)
    private boolean priceTtc;

    /** Prix unitaire HT déduit (égal au prix saisi si HT). */
    @Column(name = "ht_unit_price", precision = 12, scale = 2)
    private BigDecimal htUnitPrice;

    @Column(name = "product_snapshot", columnDefinition = "JSON")
    private String productSnapshot;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        if (this.id == null) { this.id = java.util.UUID.randomUUID(); }

        createdAt = Instant.now();
        if (discount == null) discount = BigDecimal.ZERO;
        if (vatRate == null) vatRate = new BigDecimal("19");
        if (fodecAmount == null) fodecAmount = BigDecimal.ZERO;
        if (vatBase == null) vatBase = BigDecimal.ZERO;
        if (vatAmount == null) vatAmount = BigDecimal.ZERO;
        calculateLineTotal();
    }

    @PreUpdate
    void preUpdate() {
        calculateLineTotal();
    }

    private void calculateLineTotal() {
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.lineTotal = subtotal.subtract(discount);
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public UUID getProductId() { return productId; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public String getProductRef() { return productRef; }
    public void setProductRef(String productRef) { this.productRef = productRef; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getDiscount() { return discount; }
    public void setDiscount(BigDecimal discount) { this.discount = discount; }
    public BigDecimal getLineTotal() { return lineTotal; }
    /** Reconstruction d'affichage (PDF devis) : total figé du snapshot. */
    public void setLineTotal(BigDecimal lineTotal) { this.lineTotal = lineTotal; }
    public BigDecimal getVatRate() { return vatRate; }
    public void setVatRate(BigDecimal vatRate) { this.vatRate = vatRate; }
    public BigDecimal getFodecAmount() { return fodecAmount; }
    public void setFodecAmount(BigDecimal fodecAmount) { this.fodecAmount = fodecAmount; }
    public BigDecimal getVatBase() { return vatBase; }
    public void setVatBase(BigDecimal vatBase) { this.vatBase = vatBase; }
    public BigDecimal getVatAmount() { return vatAmount; }
    public void setVatAmount(BigDecimal vatAmount) { this.vatAmount = vatAmount; }
    public boolean isFodecApplicable() { return fodecApplicable; }
    public void setFodecApplicable(boolean fodecApplicable) { this.fodecApplicable = fodecApplicable; }
    public boolean isPriceTtc() { return priceTtc; }
    public void setPriceTtc(boolean priceTtc) { this.priceTtc = priceTtc; }
    public BigDecimal getHtUnitPrice() { return htUnitPrice; }
    public void setHtUnitPrice(BigDecimal htUnitPrice) { this.htUnitPrice = htUnitPrice; }
    public String getProductSnapshot() { return productSnapshot; }
    public void setProductSnapshot(String productSnapshot) { this.productSnapshot = productSnapshot; }
    public Instant getCreatedAt() { return createdAt; }
}
