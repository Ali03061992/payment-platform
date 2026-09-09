package com.paymentplatform.payment.application.dto;

import java.util.UUID;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record SearchPaymentsResponse(
        List<PaymentSearchResult> payments,
        long total,
        int page,
        int size
) {
    public record PaymentSearchResult(
            UUID id,
            String reference,
            UUID shopId,
            String shopName,
            UUID supplierId,
            String supplierName,
            UUID createdBy,
            String createdByName,
            BigDecimal amount,
            String currency,
            String status,
            String rejectionReason,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
