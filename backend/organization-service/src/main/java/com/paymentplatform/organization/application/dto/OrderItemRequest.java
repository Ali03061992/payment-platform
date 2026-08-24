package com.paymentplatform.organization.application.dto;

import java.math.BigDecimal;

public record OrderItemRequest(Long productId, Integer quantity, BigDecimal discount) {}
