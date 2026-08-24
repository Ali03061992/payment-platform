package com.paymentplatform.organization.application.dto;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrderRequest(
        Long supplierId,
        Long shopId,
        Boolean asapPayment,
        String currency,
        String notes,
        List<OrderItemRequest> items
) {}
