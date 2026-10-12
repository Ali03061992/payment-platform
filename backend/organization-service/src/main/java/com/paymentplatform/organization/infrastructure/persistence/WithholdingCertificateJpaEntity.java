package com.paymentplatform.organization.infrastructure.persistence;

import com.paymentplatform.organization.domain.model.WithholdingCertificateStatus;
import com.paymentplatform.organization.domain.model.WithholdingNature;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "withholding_certificates")
public class WithholdingCertificateJpaEntity {

    @Id
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "order_id", nullable = false, unique = true, columnDefinition = "VARCHAR(36)")
    private UUID orderId;

    @Column(name = "number", nullable = false, unique = true, length = 32)
    private String number;

    @Column(name = "debtor_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID debtorId;

    @Column(name = "debtor_name", nullable = false, length = 100)
    private String debtorName;

    @Column(name = "debtor_matricule", length = 32)
    private String debtorMatricule;

    @Column(name = "debtor_address", length = 255)
    private String debtorAddress;

    @Column(name = "beneficiary_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID beneficiaryId;

    @Column(name = "beneficiary_name", nullable = false, length = 100)
    private String beneficiaryName;

    @Column(name = "beneficiary_matricule", length = 32)
    private String beneficiaryMatricule;

    @Column(name = "beneficiary_address", length = 255)
    private String beneficiaryAddress;

    @Column(nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private WithholdingNature nature;

    @Column(name = "gross_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal grossAmount;

    @Column(name = "rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal rate;

    @Column(name = "withholding_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal withholdingAmount;

    @Column(name = "net_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal netAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "order_reference", nullable = false, length = 64)
    private String orderReference;

    @Column(name = "order_date", nullable = false)
    private LocalDate orderDate;

    @Column(name = "billing_period", length = 16)
    private String billingPeriod;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private WithholdingCertificateStatus status;

    @Column(name = "tej_reference", length = 64)
    private String tejReference;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }
    public String getNumber() { return number; }
    public void setNumber(String number) { this.number = number; }
    public UUID getDebtorId() { return debtorId; }
    public void setDebtorId(UUID debtorId) { this.debtorId = debtorId; }
    public String getDebtorName() { return debtorName; }
    public void setDebtorName(String debtorName) { this.debtorName = debtorName; }
    public String getDebtorMatricule() { return debtorMatricule; }
    public void setDebtorMatricule(String debtorMatricule) { this.debtorMatricule = debtorMatricule; }
    public String getDebtorAddress() { return debtorAddress; }
    public void setDebtorAddress(String debtorAddress) { this.debtorAddress = debtorAddress; }
    public UUID getBeneficiaryId() { return beneficiaryId; }
    public void setBeneficiaryId(UUID beneficiaryId) { this.beneficiaryId = beneficiaryId; }
    public String getBeneficiaryName() { return beneficiaryName; }
    public void setBeneficiaryName(String beneficiaryName) { this.beneficiaryName = beneficiaryName; }
    public String getBeneficiaryMatricule() { return beneficiaryMatricule; }
    public void setBeneficiaryMatricule(String v) { this.beneficiaryMatricule = v; }
    public String getBeneficiaryAddress() { return beneficiaryAddress; }
    public void setBeneficiaryAddress(String v) { this.beneficiaryAddress = v; }
    public WithholdingNature getNature() { return nature; }
    public void setNature(WithholdingNature nature) { this.nature = nature; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }
    public BigDecimal getWithholdingAmount() { return withholdingAmount; }
    public void setWithholdingAmount(BigDecimal v) { this.withholdingAmount = v; }
    public BigDecimal getNetAmount() { return netAmount; }
    public void setNetAmount(BigDecimal netAmount) { this.netAmount = netAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getOrderReference() { return orderReference; }
    public void setOrderReference(String v) { this.orderReference = v; }
    public LocalDate getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDate orderDate) { this.orderDate = orderDate; }
    public String getBillingPeriod() { return billingPeriod; }
    public void setBillingPeriod(String billingPeriod) { this.billingPeriod = billingPeriod; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public WithholdingCertificateStatus getStatus() { return status; }
    public void setStatus(WithholdingCertificateStatus status) { this.status = status; }
    public String getTejReference() { return tejReference; }
    public void setTejReference(String tejReference) { this.tejReference = tejReference; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @PrePersist
    protected void onCreate() {
        if (this.id == null) { this.id = java.util.UUID.randomUUID(); }
    }
}
