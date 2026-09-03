package com.paymentplatform.payment.application.dto;

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
            long id,
            String reference,
            long shopId,
            String shopName,
            long supplierId,
            String supplierName,
            long createdBy,
            String createdByName,
            BigDecimal amount,
            String currency,
            String status,
            String rejectionReason,
            Instant createdAt,
            Instant updatedAt
    ) {}
}
