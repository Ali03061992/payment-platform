package com.paymentplatform.organization.application.dto;

import java.util.UUID;

import java.math.BigDecimal;
import java.time.Instant;

public record BalanceResponse(
        UUID supplierId,
        UUID shopId,
        String supplierName,
        String shopName,
        BigDecimal currentBalance,
        BigDecimal totalOrders,
        BigDecimal totalPayments,
        BigDecimal remainingDue,
        Instant lastTransaction
) {}
