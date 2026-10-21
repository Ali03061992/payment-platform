package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.AddDisputeMessageRequest;
import com.paymentplatform.organization.application.dto.CreateDisputeRequest;
import com.paymentplatform.organization.application.dto.DisputeResponse;
import com.paymentplatform.organization.application.usecase.AddDisputeMessageUseCase;
import com.paymentplatform.organization.application.usecase.CreateDisputeUseCase;
import com.paymentplatform.organization.application.usecase.ResolveDisputeUseCase;
import com.paymentplatform.organization.domain.model.Dispute;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.repository.DisputeRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/disputes")
public class DisputeController {

    private final CreateDisputeUseCase createDispute;
    private final AddDisputeMessageUseCase addDisputeMessage;
    private final ResolveDisputeUseCase resolveDispute;
    private final DisputeRepository disputeRepository;
    private final OrderRepository orderRepository;
    private final OrganizationRepository organizationRepository;

    public DisputeController(CreateDisputeUseCase createDispute,
                             AddDisputeMessageUseCase addDisputeMessage,
                             ResolveDisputeUseCase resolveDispute,
                             DisputeRepository disputeRepository,
                             OrderRepository orderRepository,
                             OrganizationRepository organizationRepository) {
        this.createDispute = createDispute;
        this.addDisputeMessage = addDisputeMessage;
        this.resolveDispute = resolveDispute;
        this.disputeRepository = disputeRepository;
        this.orderRepository = orderRepository;
        this.organizationRepository = organizationRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SHOP_MANAGER')")
    public ResponseEntity<DisputeResponse> createDispute(@Valid @RequestBody CreateDisputeRequest request) {
        var current = CurrentUser.get();
        Order order = orderRepository.findById(request.orderId())
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + request.orderId()));
        if (!current.roles().contains("SYSTEM_ADMIN")
                && (current.organizationId() == null
                || (!current.organizationId().equals(order.getShopId())
                && !current.organizationId().equals(order.getSupplierId())))) {
            throw new ForbiddenException("Accès refusé à cette commande");
        }
        String role = current.roles().stream().anyMatch(r -> r.startsWith("SUPPLIER")) ? "SUPPLIER" : "SHOP";
        DisputeResponse response = createDispute.execute(request.orderId(), current.userId(), role, request.reason());
        return ResponseEntity.created(URI.create("/api/disputes/" + response.id()))
                .body(enrich(response));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<DisputeResponse>> getDisputesByOrder(@PathVariable UUID orderId) {
        var current = CurrentUser.get();
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        if (!current.roles().contains("SYSTEM_ADMIN")
                && (current.organizationId() == null
                || (!current.organizationId().equals(order.getShopId())
                && !current.organizationId().equals(order.getSupplierId())))) {
            throw new ForbiddenException("Accès refusé à cette commande");
        }
        var disputes = disputeRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        return ResponseEntity.ok(disputes.stream().map(this::enrich).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<DisputeResponse> getDispute(@PathVariable UUID id) {
        return ResponseEntity.ok(enrich(access(id)));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER')")
    public ResponseEntity<DisputeResponse> addMessage(
            @PathVariable UUID id,
            @Valid @RequestBody AddDisputeMessageRequest request) {
        access(id);
        var current = CurrentUser.get();
        String role = current.roles().contains("SUPPLIER_ADMIN") || current.roles().contains("SUPPLIER_AGENT")
                ? "SUPPLIER" : "SHOP";
        DisputeResponse response = addDisputeMessage.execute(id, current.userId(), role, request.content());
        return ResponseEntity.ok(enrich(response));
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        access(id);
        var current = CurrentUser.get();
        String targetStatus = body.getOrDefault("status", "RESOLVED");
        DisputeResponse response = resolveDispute.execute(id, current.userId(), targetStatus);
        return ResponseEntity.ok(enrich(response));
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<DisputeResponse>> listDisputes(
            @RequestParam(required = false) String status) {
        var current = CurrentUser.get();
        List<Dispute> disputes;
        if (current.roles().contains("SYSTEM_ADMIN")) {
            disputes = (status != null && !status.isBlank())
                    ? disputeRepository.findByStatusOrderByCreatedAtDesc(status)
                    : disputeRepository.findAll();
        } else {
            if (current.organizationId() == null) {
                throw new ForbiddenException("Aucune organisation associée à cet utilisateur");
            }
            Map<UUID, Dispute> unique = new LinkedHashMap<>();
            disputeRepository.findByShopIdOrderByCreatedAtDesc(current.organizationId())
                    .forEach(d -> unique.put(d.getId(), d));
            disputeRepository.findBySupplierIdOrderByCreatedAtDesc(current.organizationId())
                    .forEach(d -> unique.put(d.getId(), d));
            disputes = new ArrayList<>(unique.values());
            if (status != null && !status.isBlank()) {
                disputes.removeIf(d -> !status.equals(d.getStatus()));
            }
            disputes.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        }
        return ResponseEntity.ok(disputes.stream().map(this::enrich).toList());
    }

    private Dispute access(UUID id) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Litige non trouvé : " + id));
        var current = CurrentUser.get();
        if (!current.roles().contains("SYSTEM_ADMIN")
                && (current.organizationId() == null
                || (!current.organizationId().equals(dispute.getShopId())
                && !current.organizationId().equals(dispute.getSupplierId())))) {
            throw new ForbiddenException("Accès refusé à ce litige");
        }
        return dispute;
    }

    private DisputeResponse enrich(Dispute dispute) {
        return enrich(DisputeResponse.from(dispute));
    }

    private DisputeResponse enrich(DisputeResponse response) {
        String orderReference = orderRepository.findById(response.orderId())
                .map(Order::getReference).orElse(null);
        String supplierName = organizationRepository.findById(OrganizationId.of(response.supplierId()))
                .map(org -> org.name().value()).orElse(null);
        String shopName = organizationRepository.findById(OrganizationId.of(response.shopId()))
                .map(org -> org.name().value()).orElse(null);
        return new DisputeResponse(response.id(), response.orderId(), response.shopId(),
                response.supplierId(), response.openedBy(), response.status(), response.reason(),
                response.messages(), response.version(), response.createdAt(), response.updatedAt(),
                orderReference, supplierName, shopName);
    }
}
