package com.paymentplatform.organization.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;

public record UpdateOrderRequest(
        String notes,
        Boolean asapPayment,
        @NotNull(message = "La commande doit contenir au moins un article") List<OrderItemRequest> items
) {}
