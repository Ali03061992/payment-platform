package com.paymentplatform.organization.application.dto;

import java.util.UUID;

import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderItem;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record OrderResponse(
        UUID id,
        String reference,
        UUID supplierId,
        UUID shopId,
        String supplierName,
        String shopName,
        UUID createdBy,
        String createdByRole,
        String source,
        String status,
        BigDecimal subtotal,
        BigDecimal taxRate,
        BigDecimal taxAmount,
        BigDecimal total,
        String currency,
        UUID deliveryAgentId,
        UUID receivedBy,
        Instant receivedAt,
        Instant deliveredAt,
        LocalDate plannedDeliveryDate,
        LocalDate confirmedDeliveryDate,
        boolean asapPayment,
        String notes,
        Long version,
        Instant createdAt,
        Instant updatedAt,
        List<OrderItemResponse> items
) {
    public static OrderResponse from(Order order, List<OrderItem> orderItems) {
        return from(order, orderItems, null, null);
    }

    public static OrderResponse from(Order order, List<OrderItem> orderItems, String supplierName, String shopName) {
        List<OrderItemResponse> items = orderItems.stream()
                .map(OrderItemResponse::from)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getReference(),
                order.getSupplierId(),
                order.getShopId(),
                supplierName,
                shopName,
                order.getCreatedBy(),
                order.getCreatedByRole(),
                order.getSource(),
                order.getStatus(),
                order.getSubtotal(),
                order.getTaxRate(),
                order.getTaxAmount(),
                order.getTotal(),
                order.getCurrency(),
                order.getDeliveryAgentId(),
                order.getReceivedBy(),
                order.getReceivedAt(),
                order.getDeliveredAt(),
                order.getPlannedDeliveryDate(),
                order.getConfirmedDeliveryDate(),
                order.isAsapPayment(),
                order.getNotes(),
                order.getVersion(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                items
        );
    }

    public record OrderItemResponse(
            UUID id,
            UUID productId,
            String productRef,
            String productName,
            Integer quantity,
            BigDecimal unitPrice,
            BigDecimal discount,
            BigDecimal lineTotal,
            Instant createdAt
    ) {
        public static OrderItemResponse from(OrderItem item) {
            return new OrderItemResponse(
                    item.getId(),
                    item.getProductId(),
                    item.getProductRef(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getDiscount(),
                    item.getLineTotal(),
                    item.getCreatedAt()
            );
        }
    }
}
