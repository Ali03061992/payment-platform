package com.paymentplatform.payment.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AgentPaymentSummary(
        UUID userId,
        String username,
        long paymentCount,
        BigDecimal totalAmount,
        BigDecimal confirmedTotal,
        String currency,
        List<PaymentResponse> payments
) {}
