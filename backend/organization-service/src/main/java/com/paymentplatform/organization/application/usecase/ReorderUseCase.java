package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class ReorderUseCase {

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final CreateOrderUseCase createOrder;

    public ReorderUseCase(OrderRepository orders, OrderItemRepository orderItems, CreateOrderUseCase createOrder) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.createOrder = createOrder;
    }

    public OrderResponse execute(UUID orderId, UUID actorUserId, String actorRole) {
        var order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));

        var items = orderItems.findByOrderId(orderId);
        if (items.isEmpty()) {
            throw new NotFoundException("La commande originale ne contient aucun article");
        }

        List<OrderItemRequest> itemRequests = items.stream()
                .map(item -> new OrderItemRequest(item.getProductId(), item.getQuantity(), item.getDiscount()))
                .toList();

        CreateOrderRequest request = new CreateOrderRequest(
                order.getSupplierId(),
                order.getShopId(),
                order.isAsapPayment(),
                order.getPaymentTerms(),
                order.getCurrency(),
                order.getGlobalDiscount(),
                order.getTaxRate(),
                "Recommandé depuis " + order.getReference(),
                itemRequests
        );

        return createOrder.execute(request, actorUserId, actorRole);
    }
}
