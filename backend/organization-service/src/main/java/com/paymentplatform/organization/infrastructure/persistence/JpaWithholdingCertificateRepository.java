package com.paymentplatform.organization.infrastructure.persistence;

import com.paymentplatform.organization.domain.model.WithholdingCertificate;
import com.paymentplatform.organization.domain.repository.WithholdingCertificateRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class JpaWithholdingCertificateRepository implements WithholdingCertificateRepository {

    private final WithholdingCertificateJpaRepository jpa;

    public JpaWithholdingCertificateRepository(WithholdingCertificateJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    @Transactional
    public WithholdingCertificate save(WithholdingCertificate certificate) {
        return toDomain(jpa.save(toEntity(certificate)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WithholdingCertificate> findById(UUID id) {
        return jpa.findById(id).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WithholdingCertificate> findByOrderId(UUID orderId) {
        return jpa.findByOrderId(orderId).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WithholdingCertificate> findByNumber(String number) {
        return jpa.findByNumber(number).map(this::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithholdingCertificate> findByBeneficiaryId(UUID beneficiaryId) {
        return jpa.findByBeneficiaryId(beneficiaryId).stream().map(this::toDomain).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<WithholdingCertificate> findByDebtorId(UUID debtorId) {
        return jpa.findByDebtorId(debtorId).stream().map(this::toDomain).toList();
    }

    private WithholdingCertificateJpaEntity toEntity(WithholdingCertificate c) {
        WithholdingCertificateJpaEntity e = new WithholdingCertificateJpaEntity();
        e.setId(c.id());
        e.setOrderId(c.orderId());
        e.setNumber(c.number());
        e.setDebtorId(c.debtorId());
        e.setDebtorName(c.debtorName());
        e.setDebtorMatricule(c.debtorMatricule());
        e.setDebtorAddress(c.debtorAddress());
        e.setBeneficiaryId(c.beneficiaryId());
        e.setBeneficiaryName(c.beneficiaryName());
        e.setBeneficiaryMatricule(c.beneficiaryMatricule());
        e.setBeneficiaryAddress(c.beneficiaryAddress());
        e.setNature(c.nature());
        e.setGrossAmount(c.grossAmount());
        e.setRate(c.rate());
        e.setWithholdingAmount(c.withholdingAmount());
        e.setNetAmount(c.netAmount());
        e.setCurrency(c.currency());
        e.setOrderReference(c.orderReference());
        e.setOrderDate(c.orderDate());
        e.setBillingPeriod(c.billingPeriod());
        e.setDueDate(c.dueDate());
        e.setStatus(c.status());
        e.setTejReference(c.tejReference());
        e.setCreatedAt(c.createdAt());
        e.setUpdatedAt(c.updatedAt());
        return e;
    }

    private WithholdingCertificate toDomain(WithholdingCertificateJpaEntity e) {
        return WithholdingCertificate.builder()
                .id(e.getId()).orderId(e.getOrderId()).number(e.getNumber())
                .debtorId(e.getDebtorId()).debtorName(e.getDebtorName())
                .debtorMatricule(e.getDebtorMatricule()).debtorAddress(e.getDebtorAddress())
                .beneficiaryId(e.getBeneficiaryId()).beneficiaryName(e.getBeneficiaryName())
                .beneficiaryMatricule(e.getBeneficiaryMatricule()).beneficiaryAddress(e.getBeneficiaryAddress())
                .nature(e.getNature()).grossAmount(e.getGrossAmount()).rate(e.getRate())
                .withholdingAmount(e.getWithholdingAmount()).netAmount(e.getNetAmount())
                .currency(e.getCurrency()).orderReference(e.getOrderReference())
                .orderDate(e.getOrderDate()).billingPeriod(e.getBillingPeriod())
                .dueDate(e.getDueDate()).status(e.getStatus()).tejReference(e.getTejReference())
                .createdAt(e.getCreatedAt()).updatedAt(e.getUpdatedAt())
                .build();
    }
}
