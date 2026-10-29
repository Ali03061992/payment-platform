package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.application.security.TenantAccess;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.domain.security.ActorContext;
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

    public OrderResponse execute(UUID orderId, ActorContext actor) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        TenantAccess.requireShop(actor, order.getShopId());
        return reorder(order, actor);
    }

    @Deprecated
    public OrderResponse execute(UUID orderId, UUID actorUserId, String actorRole) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        ActorContext actor = new ActorContext(actorUserId, order.getShopId(),
                List.of(RoleCode.SHOP_ADMIN.name()));
        return reorder(order, actor);
    }

    private OrderResponse reorder(Order order, ActorContext actor) {
        var items = orderItems.findByOrderId(order.getId());
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
                order.getTaxRate(),
                "Recommandé depuis " + order.getReference(),
                itemRequests
        );

        return createOrder.execute(request, actor);
    }
}
