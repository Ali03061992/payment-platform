package com.paymentplatform.organization.application.dto;

import com.paymentplatform.organization.domain.model.QuoteRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record QuoteRequestResponse(
        UUID id,
        String reference,
        UUID supplierId,
        String supplierName,
        UUID shopId,
        String shopName,
        String notes,
        String status,
        String linesJson,
        UUID createdBy,
        Instant createdAt,
        List<SupplierQuoteResponse> quotes) {

    public static QuoteRequestResponse from(QuoteRequest r, List<SupplierQuoteResponse> quotes) {
        return new QuoteRequestResponse(r.id(), r.reference(), r.supplierId(), r.supplierName(),
                r.shopId(), r.shopName(), r.notes(), r.status().name(), r.linesJson(),
                r.createdBy(), r.createdAt(), quotes);
    }
}
