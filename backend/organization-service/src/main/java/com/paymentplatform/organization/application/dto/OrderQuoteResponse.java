package com.paymentplatform.organization.application.dto;

import com.paymentplatform.organization.domain.model.OrderQuote;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderQuoteResponse(
        UUID id,
        UUID orderId,
        String orderReference,
        UUID supplierId,
        int version,
        BigDecimal globalDiscount,
        BigDecimal subtotal,
        BigDecimal taxAmount,
        BigDecimal total,
        BigDecimal stampAmount,
        String currency,
        String linesJson,
        UUID createdBy,
        Instant createdAt) {

    public static OrderQuoteResponse from(OrderQuote q) {
        return new OrderQuoteResponse(q.id(), q.orderId(), q.orderReference(), q.supplierId(),
                q.version(), q.globalDiscount(), q.subtotal(), q.taxAmount(), q.total(),
                q.stampAmount(), q.currency(), q.linesJson(), q.createdBy(), q.createdAt());
    }
}
