package com.paymentplatform.payment.application.dto;

import java.time.Instant;

public record SearchPaymentsRequest(
        Long shopId,
        Long supplierId,
        Long createdBy,
        String status,
        Instant from,
        Instant to,
        int page,
        int size
) {
    public SearchPaymentsRequest {
        if (page < 0) page = 0;
        if (size < 1 || size > 100) size = 20;
    }
}
