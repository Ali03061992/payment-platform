package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.application.usecase.*;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final CreateOrderUseCase createOrder;
    private final ConfirmOrderUseCase confirmOrder;
    private final PrepareOrderUseCase prepareOrder;
    private final DeliverOrderUseCase deliverOrder;
    private final AcceptOrderUseCase acceptOrder;
    private final CancelOrderUseCase cancelOrder;
    private final RejectOrderUseCase rejectOrder;
    private final DeliveryRejectOrderUseCase deliveryRejectOrder;
    private final com.paymentplatform.organization.domain.repository.OrderRepository orderRepository;
    private final com.paymentplatform.organization.domain.repository.OrderItemRepository orderItemRepository;

    public OrderController(CreateOrderUseCase createOrder,
                           ConfirmOrderUseCase confirmOrder,
                           PrepareOrderUseCase prepareOrder,
                           DeliverOrderUseCase deliverOrder,
                           AcceptOrderUseCase acceptOrder,
                           CancelOrderUseCase cancelOrder,
                           RejectOrderUseCase rejectOrder,
                           DeliveryRejectOrderUseCase deliveryRejectOrder,
                           com.paymentplatform.organization.domain.repository.OrderRepository orderRepository,
                           com.paymentplatform.organization.domain.repository.OrderItemRepository orderItemRepository) {
        this.createOrder = createOrder;
        this.confirmOrder = confirmOrder;
        this.prepareOrder = prepareOrder;
        this.deliverOrder = deliverOrder;
        this.acceptOrder = acceptOrder;
        this.cancelOrder = cancelOrder;
        this.rejectOrder = rejectOrder;
        this.deliveryRejectOrder = deliveryRejectOrder;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SHOP_MANAGER')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        var current = CurrentUser.get();
        String role = current.roles().contains("SUPPLIER_ADMIN") ? "SUPPLIER" : "SHOP";
        OrderResponse response = createOrder.execute(request, current.userId(), role);
        return ResponseEntity.created(URI.create("/api/orders/" + response.id())).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SHOP_MANAGER', 'DELIVERY_AGENT', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<OrderResponse>> listOrders(@RequestParam(required = false) String status) {
        var current = CurrentUser.get();
        List<com.paymentplatform.organization.domain.model.Order> orders;

        if ((current.roles().contains("SUPPLIER_ADMIN") || current.roles().contains("SUPPLIER_AGENT")) && current.organizationId() != null) {
            orders = (status != null && !status.isBlank())
                    ? orderRepository.findBySupplierIdAndStatus(current.organizationId(), status)
                    : orderRepository.findBySupplierId(current.organizationId());
        } else if ((current.roles().contains("SHOP_MANAGER") || current.roles().contains("SHOP_ADMIN")) && current.organizationId() != null) {
            orders = (status != null && !status.isBlank())
                    ? orderRepository.findByShopIdAndStatus(current.organizationId(), status)
                    : orderRepository.findByShopId(current.organizationId());
        } else if (current.roles().contains("DELIVERY_AGENT")) {
            orders = orderRepository.findByDeliveryAgentId(current.userId());
        } else {
            orders = orderRepository.findAll();
        }

        List<OrderResponse> responses = orders.stream()
                .map(o -> {
                    var items = orderItemRepository.findByOrderId(o.getId());
                    return OrderResponse.from(o, items);
                })
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SHOP_MANAGER', 'DELIVERY_AGENT', 'SYSTEM_ADMIN')")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable Long id) {
        var current = CurrentUser.get();
        var order = orderRepository.findById(id);
        if (order.isEmpty()) return ResponseEntity.notFound().build();
        var o = order.get();
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            if (current.organizationId() == null) return ResponseEntity.status(403).build();
            boolean isSupplier = o.getSupplierId() != null && o.getSupplierId().equals(current.organizationId());
            boolean isShop = o.getShopId() != null && o.getShopId().equals(current.organizationId());
            boolean isDeliveryAgent = o.getDeliveryAgentId() != null && o.getDeliveryAgentId().equals(current.userId());
            if (!isSupplier && !isShop && !isDeliveryAgent) return ResponseEntity.status(403).build();
        }
        var items = orderItemRepository.findByOrderId(id);
        return ResponseEntity.ok(OrderResponse.from(o, items));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN')")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable Long id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(confirmOrder.execute(id, current.userId()));
    }

    @PostMapping("/{id}/prepare")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN')")
    public ResponseEntity<OrderResponse> prepareOrder(@PathVariable Long id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(prepareOrder.execute(id, current.userId()));
    }

    @PostMapping("/{id}/ready")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN')")
    public ResponseEntity<OrderResponse> readyForDelivery(@PathVariable Long id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(prepareOrder.readyForDelivery(id, current.userId()));
    }

    @PostMapping("/{id}/assign-delivery")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN')")
    public ResponseEntity<OrderResponse> assignDeliveryAgent(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        var order = orderRepository.findById(id);
        if (order.isEmpty()) return ResponseEntity.notFound().build();
        order.get().assignDeliveryAgent(body.get("agentId"));
        orderRepository.save(order.get());
        var items = orderItemRepository.findByOrderId(id);
        return ResponseEntity.ok(OrderResponse.from(order.get(), items));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'DELIVERY_AGENT')")
    public ResponseEntity<OrderResponse> deliverOrder(
            @PathVariable Long id,
            @RequestBody Map<String, Long> body) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(deliverOrder.execute(id, body.get("receivedBy"), current.userId()));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyAuthority('SHOP_MANAGER')")
    public ResponseEntity<OrderResponse> acceptOrder(@PathVariable Long id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(acceptOrder.execute(id, current.userId()));
    }

    @PostMapping("/{id}/accept-asap")
    @PreAuthorize("hasAnyAuthority('SHOP_MANAGER')")
    public ResponseEntity<OrderResponse> acceptAsapOrder(@PathVariable Long id) {
        var current = CurrentUser.get();
        OrderResponse response = acceptOrder.execute(id, current.userId());
        // TODO: auto-create payment for ASAP orders
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SHOP_MANAGER')")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable Long id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(cancelOrder.execute(id, current.userId()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('SHOP_MANAGER', 'SHOP_ADMIN')")
    public ResponseEntity<OrderResponse> rejectOrder(@PathVariable Long id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(rejectOrder.execute(id, current.userId()));
    }

    @PostMapping("/{id}/delivery-reject")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'DELIVERY_AGENT')")
    public ResponseEntity<OrderResponse> deliveryRejectOrder(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        var current = CurrentUser.get();
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(deliveryRejectOrder.execute(id, current.userId(), reason));
    }

    @GetMapping("/my-deliveries")
    @PreAuthorize("hasAnyAuthority('DELIVERY_AGENT')")
    public ResponseEntity<List<OrderResponse>> myDeliveries() {
        var current = CurrentUser.get();
        var orders = orderRepository.findByDeliveryAgentId(current.userId());
        List<OrderResponse> responses = orders.stream()
                .map(o -> {
                    var items = orderItemRepository.findByOrderId(o.getId());
                    return OrderResponse.from(o, items);
                })
                .toList();
        return ResponseEntity.ok(responses);
    }
}
