package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.CreateOrderCommentRequest;
import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.application.dto.PageResponse;
import com.paymentplatform.organization.application.dto.UpdateOrderRequest;
import com.paymentplatform.organization.application.service.InvoicePdfService;
import com.paymentplatform.organization.application.usecase.*;
import com.paymentplatform.organization.domain.model.OrderComment;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.repository.OrderCommentRepository;
import com.paymentplatform.organization.domain.repository.OrderEventRepository;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.organization.infrastructure.http.IdentityClient;
import com.paymentplatform.organization.infrastructure.http.PaymentClient;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
    private final ConfirmDeliveryUseCase confirmDeliveryUseCase;
    private final AcceptDeliveryUseCase acceptDeliveryUseCase;
    private final PaymentClient paymentClient;
    private final IdentityClient identityClient;
    private final com.paymentplatform.organization.domain.repository.OrderRepository orderRepository;
    private final com.paymentplatform.organization.domain.repository.OrderItemRepository orderItemRepository;
    private final OrganizationRepository organizationRepository;
    private final OrderEventRepository orderEventRepository;
    private final com.paymentplatform.organization.infrastructure.csv.OrderCsvExportService orderCsvExportService;
    private final InvoicePdfService invoicePdfService;
    private final ReorderUseCase reorderUseCase;
    private final UpdateOrderUseCase updateOrderUseCase;
    private final OrderCommentRepository orderCommentRepository;
    private final OutboxEventStore outbox;

    public OrderController(CreateOrderUseCase createOrder,
                           ConfirmOrderUseCase confirmOrder,
                           PrepareOrderUseCase prepareOrder,
                           DeliverOrderUseCase deliverOrder,
                           AcceptOrderUseCase acceptOrder,
                           CancelOrderUseCase cancelOrder,
                           RejectOrderUseCase rejectOrder,
                           DeliveryRejectOrderUseCase deliveryRejectOrder,
                           ConfirmDeliveryUseCase confirmDeliveryUseCase,
                           AcceptDeliveryUseCase acceptDeliveryUseCase,
                           PaymentClient paymentClient,
                           IdentityClient identityClient,
                           com.paymentplatform.organization.domain.repository.OrderRepository orderRepository,
                           com.paymentplatform.organization.domain.repository.OrderItemRepository orderItemRepository,
                           OrganizationRepository organizationRepository,
                           OrderEventRepository orderEventRepository,
                           com.paymentplatform.organization.infrastructure.csv.OrderCsvExportService orderCsvExportService,
                           InvoicePdfService invoicePdfService,
                           ReorderUseCase reorderUseCase,
                           UpdateOrderUseCase updateOrderUseCase,
                           OrderCommentRepository orderCommentRepository,
                           OutboxEventStore outbox) {
        this.createOrder = createOrder;
        this.confirmOrder = confirmOrder;
        this.prepareOrder = prepareOrder;
        this.deliverOrder = deliverOrder;
        this.acceptOrder = acceptOrder;
        this.cancelOrder = cancelOrder;
        this.rejectOrder = rejectOrder;
        this.deliveryRejectOrder = deliveryRejectOrder;
        this.confirmDeliveryUseCase = confirmDeliveryUseCase;
        this.acceptDeliveryUseCase = acceptDeliveryUseCase;
        this.paymentClient = paymentClient;
        this.identityClient = identityClient;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.organizationRepository = organizationRepository;
        this.orderEventRepository = orderEventRepository;
        this.orderCsvExportService = orderCsvExportService;
        this.invoicePdfService = invoicePdfService;
        this.reorderUseCase = reorderUseCase;
        this.updateOrderUseCase = updateOrderUseCase;
        this.orderCommentRepository = orderCommentRepository;
        this.outbox = outbox;
    }

    private String resolveOrgName(UUID orgId) {
        if (orgId == null) return null;
        return organizationRepository.findById(OrganizationId.of(orgId))
                .map(org -> org.name().value())
                .orElse(null);
    }

    private String resolveUserName(UUID userId) {
        return identityClient.resolveUserName(userId);
    }

    private String resolveEventActor(List<OrderEvent> events, String action) {
        return events.stream()
                .filter(e -> action.equals(e.getAction()))
                .findFirst()
                .map(e -> resolveUserName(e.getUserId()))
                .orElse(null);
    }

    private OrderResponse buildOrderResponse(com.paymentplatform.organization.domain.model.Order o) {
        var items = orderItemRepository.findByOrderId(o.getId());
        var events = orderEventRepository.findByOrderIdOrderByTimestampDesc(o.getId());

        return OrderResponse.from(o, items,
                resolveOrgName(o.getSupplierId()),
                resolveOrgName(o.getShopId()),
                identityClient.resolveUserName(o.getDeliveryAgentId()),
                identityClient.resolveUserName(o.getReceivedBy()),
                identityClient.resolveUserName(o.getCreatedBy()),
                resolveEventActor(events, "ORDER_CONFIRMED"),
                resolveEventActor(events, "ORDER_PREPARING"),
                resolveEventActor(events, "ORDER_READY_FOR_DELIVERY"),
                resolveEventActor(events, "ORDER_DELIVERY_ASSIGNED"),
                resolveEventActor(events, "ORDER_DELIVERY_ACCEPTED"),
                resolveEventActor(events, "ORDER_DELIVERY_CONFIRMED"),
                resolveEventActor(events, "ORDER_DELIVERED"));
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SHOP_MANAGER')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        var current = CurrentUser.get();
        String role = current.roles().contains("SUPPLIER_ADMIN") ? "SUPPLIER" : "SHOP";
        OrderResponse response = createOrder.execute(request, current.userId(), role);
        return ResponseEntity.created(URI.create("/api/orders/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SHOP_MANAGER')")
    public ResponseEntity<OrderResponse> updateOrder(@PathVariable UUID id, @Valid @RequestBody UpdateOrderRequest request) {
        var current = CurrentUser.get();
        OrderResponse response = updateOrderUseCase.execute(id, request, current.userId());
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<PageResponse<OrderResponse>> listOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var current = CurrentUser.get();
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<com.paymentplatform.organization.domain.model.Order> orderPage;

        if ((current.roles().contains("SUPPLIER_ADMIN") || current.roles().contains("SUPPLIER_AGENT")) && current.organizationId() != null) {
            orderPage = (status != null && !status.isBlank())
                    ? orderRepository.findBySupplierIdAndStatus(current.organizationId(), status, pageable)
                    : orderRepository.findBySupplierId(current.organizationId(), pageable);
        } else if ((current.roles().contains("SHOP_MANAGER") || current.roles().contains("SHOP_ADMIN")) && current.organizationId() != null) {
            orderPage = (status != null && !status.isBlank())
                    ? orderRepository.findByShopIdAndStatus(current.organizationId(), status, pageable)
                    : orderRepository.findByShopId(current.organizationId(), pageable);
        } else if (current.roles().contains("SUPPLIER_AGENT")) {
            List<com.paymentplatform.organization.domain.model.Order> orders = orderRepository.findByDeliveryAgentId(current.userId());
            List<OrderResponse> responses = orders.stream()
                    .map(this::buildOrderResponse)
                    .toList();
            return ResponseEntity.ok(new PageResponse<>(responses, responses.size(), 1, 0));
        } else {
            return ResponseEntity.ok(PageResponse.of(orderRepository.findAll(pageable).map(this::buildOrderResponse)));
        }

        return ResponseEntity.ok(PageResponse.of(orderPage.map(this::buildOrderResponse)));
    }

    @GetMapping("/deliveries")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<OrderResponse>> listDeliveries(
            @RequestParam(required = false) UUID agentId) {
        var current = CurrentUser.get();
        List<com.paymentplatform.organization.domain.model.Order> orders;

        if (agentId != null) {
            orders = orderRepository.findByDeliveryAgentId(agentId);
        } else if (current.roles().contains("SUPPLIER_AGENT")) {
            orders = orderRepository.findByDeliveryAgentId(current.userId());
        } else if (current.organizationId() != null) {
            orders = orderRepository.findBySupplierId(current.organizationId()).stream()
                    .filter(o -> o.getDeliveryAgentId() != null)
                    .toList();
        } else {
            orders = List.of();
        }

        List<OrderResponse> responses = orders.stream()
                .map(this::buildOrderResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/shop-agents")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<Map<String, String>>> getShopAgents(@RequestParam UUID shopId) {
        var users = identityClient.getUsersByOrganization(shopId);
        List<Map<String, String>> agents = new java.util.ArrayList<>();
        if (users != null && users.isArray()) {
            for (var user : users) {
                String firstName = user.has("firstName") ? user.get("firstName").asText() : "";
                String lastName = user.has("lastName") ? user.get("lastName").asText() : "";
                String id = user.has("id") ? user.get("id").asText() : "";
                // Rôles exposés pour distinguer admins/agents côté livraison.
                StringBuilder roles = new StringBuilder();
                if (user.has("roles") && user.get("roles").isArray()) {
                    for (var role : user.get("roles")) {
                        if (!roles.isEmpty()) roles.append(",");
                        roles.append(role.asText());
                    }
                }
                agents.add(Map.of("id", id, "name", (firstName + " " + lastName).trim(),
                        "roles", roles.toString()));
            }
        }
        return ResponseEntity.ok(agents);
    }

    @GetMapping("/recent")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<OrderResponse>> recentOrders(
            @RequestParam(defaultValue = "5") int limit) {
        var current = CurrentUser.get();
        var pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        List<com.paymentplatform.organization.domain.model.Order> orders;

        if ((current.roles().contains("SUPPLIER_ADMIN") || current.roles().contains("SUPPLIER_AGENT")) && current.organizationId() != null) {
            orders = orderRepository.findTopNBySupplierIdOrderByCreatedAtDesc(current.organizationId(), pageable);
        } else if ((current.roles().contains("SHOP_MANAGER") || current.roles().contains("SHOP_ADMIN")) && current.organizationId() != null) {
            orders = orderRepository.findTopNByShopIdOrderByCreatedAtDesc(current.organizationId(), pageable);
        } else {
            orders = orderRepository.findAll(pageable).getContent();
        }

        List<OrderResponse> responses = orders.stream()
                .map(this::buildOrderResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID id) {
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
        return ResponseEntity.ok(buildOrderResponse(o));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<PageResponse<OrderResponse>> searchOrders(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var current = CurrentUser.get();
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        org.springframework.data.domain.Page<com.paymentplatform.organization.domain.model.Order> orderPage;

        if ((current.roles().contains("SUPPLIER_ADMIN") || current.roles().contains("SUPPLIER_AGENT")) && current.organizationId() != null) {
            orderPage = orderRepository.searchBySupplierId(q, current.organizationId(), pageable);
        } else if ((current.roles().contains("SHOP_MANAGER") || current.roles().contains("SHOP_ADMIN")) && current.organizationId() != null) {
            orderPage = orderRepository.searchByShopId(q, current.organizationId(), pageable);
        } else {
            orderPage = orderRepository.search(q, pageable);
        }

        return ResponseEntity.ok(PageResponse.of(orderPage.map(this::buildOrderResponse)));
    }

    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<OrderResponse> getOrderByReference(@PathVariable String reference) {
        var current = CurrentUser.get();
        var order = orderRepository.findByReference(reference);
        if (order.isEmpty()) return ResponseEntity.notFound().build();
        var o = order.get();
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            if (current.organizationId() == null) return ResponseEntity.status(403).build();
            boolean isSupplier = o.getSupplierId() != null && o.getSupplierId().equals(current.organizationId());
            boolean isShop = o.getShopId() != null && o.getShopId().equals(current.organizationId());
            boolean isDeliveryAgent = o.getDeliveryAgentId() != null && o.getDeliveryAgentId().equals(current.userId());
            if (!isSupplier && !isShop && !isDeliveryAgent) return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(buildOrderResponse(o));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN')")
    public ResponseEntity<OrderResponse> confirmOrder(@PathVariable UUID id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(confirmOrder.execute(id, current.userId()));
    }

    @PostMapping("/{id}/prepare")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN')")
    public ResponseEntity<OrderResponse> prepareOrder(@PathVariable UUID id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(prepareOrder.execute(id, current.userId()));
    }

    @PostMapping("/{id}/ready")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN')")
    public ResponseEntity<OrderResponse> readyForDelivery(@PathVariable UUID id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(prepareOrder.readyForDelivery(id, current.userId()));
    }

    @PostMapping("/{id}/assign-delivery")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN')")
    public ResponseEntity<OrderResponse> assignDeliveryAgent(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        var current = CurrentUser.get();
        var order = orderRepository.findById(id);
        if (order.isEmpty()) return ResponseEntity.notFound().build();
        UUID agentId = UUID.fromString(body.get("agentId").toString());
        order.get().assignDeliveryAgent(agentId);
        if (body.containsKey("plannedDeliveryDate") && body.get("plannedDeliveryDate") != null) {
            order.get().setPlannedDeliveryDate(LocalDate.parse(body.get("plannedDeliveryDate").toString()));
        }
        orderRepository.save(order.get());
        orderEventRepository.save(OrderEvent.create(id, "ORDER_DELIVERY_ASSIGNED", current.userId(),
                "Agent assigné: " + agentId));
        return ResponseEntity.ok(buildOrderResponse(order.get()));
    }

    @PostMapping("/{id}/accept-delivery")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_AGENT', 'SUPPLIER_ADMIN')")
    public ResponseEntity<OrderResponse> acceptDelivery(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        var current = CurrentUser.get();
        boolean accepted = Boolean.TRUE.equals(body.get("accepted"));
        String reason = body.containsKey("reason") ? (String) body.get("reason") : null;
        return ResponseEntity.ok(acceptDeliveryUseCase.execute(id, accepted, reason, current.userId()));
    }

    @PostMapping("/{id}/confirm-delivery")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_AGENT', 'SUPPLIER_ADMIN')")
    public ResponseEntity<OrderResponse> confirmDelivery(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        var current = CurrentUser.get();
        LocalDate confirmedDate = LocalDate.parse(body.get("confirmedDate"));
        return ResponseEntity.ok(confirmDeliveryUseCase.execute(id, confirmedDate, current.userId()));
    }

    @PostMapping("/{id}/deliver")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT')")
    public ResponseEntity<OrderResponse> deliverOrder(
            @PathVariable UUID id,
            @RequestBody Map<String, UUID> body) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(deliverOrder.execute(id, body.get("receivedBy"), current.userId()));
    }

    @PostMapping("/{id}/accept")
    @PreAuthorize("hasAnyAuthority('SHOP_MANAGER', 'SHOP_ADMIN')")
    public ResponseEntity<OrderResponse> acceptOrder(@PathVariable UUID id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(acceptOrder.execute(id, current.userId()));
    }

    @PostMapping("/{id}/accept-asap")
    @PreAuthorize("hasAnyAuthority('SHOP_MANAGER', 'SHOP_ADMIN')")
    public ResponseEntity<OrderResponse> acceptAsapOrder(@PathVariable UUID id) {
        var current = CurrentUser.get();
        OrderResponse response = acceptOrder.execute(id, current.userId());
        if (response.asapPayment()) {
            paymentClient.createAutoPayment(response.shopId(), response.supplierId(),
                    response.currency(), current.userId(), response.total(), response.id());
        }
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SHOP_MANAGER')")
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(cancelOrder.execute(id, current.userId()));
    }

    @PostMapping("/{id}/reorder")
    @PreAuthorize("hasAnyAuthority('SHOP_ADMIN', 'SHOP_MANAGER')")
    public ResponseEntity<OrderResponse> reorder(@PathVariable UUID id) {
        var current = CurrentUser.get();
        String role = "SHOP";
        return ResponseEntity.ok(reorderUseCase.execute(id, current.userId(), role));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyAuthority('SHOP_MANAGER', 'SHOP_ADMIN')")
    public ResponseEntity<OrderResponse> rejectOrder(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        var current = CurrentUser.get();
        String returnReason = body != null ? body.get("returnReason") : null;
        return ResponseEntity.ok(rejectOrder.execute(id, current.userId(), returnReason));
    }

    @PostMapping("/{id}/delivery-reject")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT')")
    public ResponseEntity<OrderResponse> deliveryRejectOrder(
            @PathVariable UUID id,
            @RequestBody(required = false) Map<String, String> body) {
        var current = CurrentUser.get();
        String reason = body != null ? body.get("reason") : null;
        return ResponseEntity.ok(deliveryRejectOrder.execute(id, current.userId(), reason));
    }

    @PostMapping("/{id}/location")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_AGENT')")
    public ResponseEntity<OrderResponse> updateLocation(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body) {
        var current = CurrentUser.get();
        var order = orderRepository.findById(id);
        if (order.isEmpty()) return ResponseEntity.notFound().build();
        var o = order.get();
        if (o.getDeliveryAgentId() == null || !o.getDeliveryAgentId().equals(current.userId())) {
            return ResponseEntity.status(403).build();
        }
        java.math.BigDecimal latitude = new java.math.BigDecimal(body.get("latitude").toString());
        java.math.BigDecimal longitude = new java.math.BigDecimal(body.get("longitude").toString());
        o.updateLocation(latitude, longitude);
        if (body.containsKey("estimatedArrival") && body.get("estimatedArrival") != null) {
            o.setEstimatedArrival(Instant.parse(body.get("estimatedArrival").toString()));
        }
        orderRepository.save(o);
        return ResponseEntity.ok(buildOrderResponse(o));
    }

    @GetMapping("/my-deliveries")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT')")
    public ResponseEntity<List<OrderResponse>> myDeliveries() {
        var current = CurrentUser.get();
        List<com.paymentplatform.organization.domain.model.Order> orders;
        if (current.roles().contains("SUPPLIER_ADMIN") && current.organizationId() != null) {
            // L'admin fournisseur voit toutes les livraisons de son organisation.
            orders = orderRepository.findBySupplierId(current.organizationId()).stream()
                    .filter(o -> o.getDeliveryAgentId() != null)
                    .toList();
        } else {
            orders = orderRepository.findByDeliveryAgentId(current.userId());
        }
        List<OrderResponse> responses = orders.stream()
                .map(this::buildOrderResponse)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{id}/comments")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<OrderComment> addComment(
            @PathVariable UUID id,
            @Valid @RequestBody CreateOrderCommentRequest request) {
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
        String authorName = resolveUserName(current.userId());
        OrderComment comment = OrderComment.create(id, current.userId(), authorName, request.content());
        OrderComment saved = orderCommentRepository.save(comment);

        orderEventRepository.save(OrderEvent.create(id, "ORDER_COMMENT", current.userId(),
                "Commentaire de " + authorName));

        outbox.append(new OrderEvents.OrderCommentCreatedEvent(
                java.util.UUID.randomUUID(), java.time.Instant.now(),
                id, o.getReference(),
                o.getShopId(), o.getSupplierId(),
                current.userId(), authorName,
                request.content(), saved.getId()),
                String.valueOf(id));

        return ResponseEntity.created(URI.create("/api/orders/" + id + "/comments/" + saved.getId()))
                .body(saved);
    }

    @GetMapping("/{id}/comments")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<OrderComment>> listComments(@PathVariable UUID id) {
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
        List<OrderComment> comments = orderCommentRepository.findByOrderIdOrderByCreatedAtAsc(id);
        return ResponseEntity.ok(comments);
    }

    @GetMapping("/export/csv")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER', 'SYSTEM_ADMIN')")
    public void exportCsv(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            HttpServletResponse response) throws Exception {
        var current = CurrentUser.get();
        UUID supplierId = null;
        UUID shopId = null;
        Instant fromInstant = null;
        Instant toInstant = null;

        if (!current.roles().contains("SYSTEM_ADMIN")) {
            if (current.organizationId() != null) {
                if (current.roles().contains("SUPPLIER_ADMIN") || current.roles().contains("SUPPLIER_AGENT")) {
                    supplierId = current.organizationId();
                } else if (current.roles().contains("SHOP_ADMIN") || current.roles().contains("SHOP_MANAGER")) {
                    shopId = current.organizationId();
                }
            }
        }

        if (dateFrom != null) {
            fromInstant = dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        if (dateTo != null) {
            toInstant = dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        }

        String csv = orderCsvExportService.generateOrdersCsv(status, fromInstant, toInstant, supplierId, shopId);
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"orders.csv\"");
        response.getWriter().write(csv);
        response.getWriter().flush();
    }

    @GetMapping("/{id}/invoice")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER', 'SYSTEM_ADMIN')")
    public void downloadInvoice(@PathVariable UUID id, HttpServletResponse response) throws Exception {
        var current = CurrentUser.get();
        var order = orderRepository.findById(id);
        if (order.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "Commande non trouvée");
            return;
        }
        var o = order.get();
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            if (current.organizationId() == null) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès refusé");
                return;
            }
            boolean isSupplier = o.getSupplierId() != null && o.getSupplierId().equals(current.organizationId());
            boolean isShop = o.getShopId() != null && o.getShopId().equals(current.organizationId());
            boolean isDeliveryAgent = o.getDeliveryAgentId() != null && o.getDeliveryAgentId().equals(current.userId());
            if (!isSupplier && !isShop && !isDeliveryAgent) {
                response.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès refusé");
                return;
            }
        }

        String status = o.getStatus();
        if (!"DELIVERED".equals(status) && !"ACCEPTED".equals(status)) {
            response.sendError(HttpServletResponse.SC_CONFLICT,
                    "La facture est disponible uniquement pour les commandes livrées ou acceptées");
            return;
        }

        var items = orderItemRepository.findByOrderId(id);
        String supplierName = resolveOrgName(o.getSupplierId());
        String shopName = resolveOrgName(o.getShopId());
        byte[] pdf = invoicePdfService.generateInvoicePdf(o, items, supplierName, shopName);

        response.setContentType("application/pdf");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"facture-" + o.getReference() + ".pdf\"");
        response.setContentLength(pdf.length);
        response.getOutputStream().write(pdf);
        response.getOutputStream().flush();
    }
}
