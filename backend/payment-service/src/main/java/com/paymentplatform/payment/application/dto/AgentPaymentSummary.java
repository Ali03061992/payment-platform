package com.paymentplatform.payment.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record AgentPaymentSummary(
        long userId,
        String username,
        long paymentCount,
        BigDecimal totalAmount,
        BigDecimal confirmedTotal,
        String currency,
        List<PaymentResponse> payments
) {}
