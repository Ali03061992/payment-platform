package com.paymentplatform.payment.application.dto;

public record PaymentStatsResponse(long total, long pending, long confirmed, long rejected, long cancelled) {}
