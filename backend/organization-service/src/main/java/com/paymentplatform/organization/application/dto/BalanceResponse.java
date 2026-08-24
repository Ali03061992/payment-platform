package com.paymentplatform.organization.application.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record BalanceResponse(
        Long supplierId,
        Long shopId,
        String supplierName,
        String shopName,
        BigDecimal currentBalance,
        BigDecimal totalOrders,
        BigDecimal totalPayments,
        BigDecimal remainingDue,
        Instant lastTransaction
) {}
