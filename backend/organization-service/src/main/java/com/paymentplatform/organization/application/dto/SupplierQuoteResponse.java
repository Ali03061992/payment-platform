package com.paymentplatform.organization.application.dto;

import com.paymentplatform.organization.domain.model.SupplierQuote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SupplierQuoteResponse(
        UUID id,
        UUID requestId,
        String requestReference,
        UUID supplierId,
        int version,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total,
        BigDecimal stampAmount,
        String currency,
        String linesJson,
        UUID createdBy,
        Instant createdAt) {

    public static SupplierQuoteResponse from(SupplierQuote q) {
        return new SupplierQuoteResponse(q.id(), q.requestId(), q.requestReference(), q.supplierId(),
                q.version(), q.subtotal(), q.taxAmount(), q.total(),
                q.stampAmount(), q.currency(), q.linesJson(), q.createdBy(), q.createdAt());
    }
}
