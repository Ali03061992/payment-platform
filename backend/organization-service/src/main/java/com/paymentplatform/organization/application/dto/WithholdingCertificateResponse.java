package com.paymentplatform.organization.application.dto;

import com.paymentplatform.organization.domain.model.WithholdingCertificate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record WithholdingCertificateResponse(
        UUID id,
        UUID orderId,
        String number,
        UUID debtorId,
        String debtorName,
        String debtorMatricule,
        String debtorAddress,
        UUID beneficiaryId,
        String beneficiaryName,
        String beneficiaryMatricule,
        String beneficiaryAddress,
        String nature,
        BigDecimal grossAmount,
        BigDecimal rate,
        BigDecimal withholdingAmount,
        BigDecimal netAmount,
        String currency,
        String orderReference,
        LocalDate orderDate,
        String billingPeriod,
        LocalDate dueDate,
        String status,
        String tejReference,
        Instant createdAt
) {
    public static WithholdingCertificateResponse from(WithholdingCertificate c) {
        return new WithholdingCertificateResponse(
                c.id(), c.orderId(), c.number(),
                c.debtorId(), c.debtorName(), c.debtorMatricule(), c.debtorAddress(),
                c.beneficiaryId(), c.beneficiaryName(), c.beneficiaryMatricule(), c.beneficiaryAddress(),
                c.nature().name(), c.grossAmount(), c.rate(), c.withholdingAmount(), c.netAmount(),
                c.currency(), c.orderReference(), c.orderDate(), c.billingPeriod(),
                c.dueDate(), c.status().name(), c.tejReference(), c.createdAt());
    }
}
