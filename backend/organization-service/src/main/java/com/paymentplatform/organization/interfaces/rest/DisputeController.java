package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.AddDisputeMessageRequest;
import com.paymentplatform.organization.application.dto.CreateDisputeRequest;
import com.paymentplatform.organization.application.dto.DisputeResponse;
import com.paymentplatform.organization.application.usecase.AddDisputeMessageUseCase;
import com.paymentplatform.organization.application.usecase.CreateDisputeUseCase;
import com.paymentplatform.organization.application.usecase.ResolveDisputeUseCase;
import com.paymentplatform.organization.domain.repository.DisputeRepository;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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

    public DisputeController(CreateDisputeUseCase createDispute,
                             AddDisputeMessageUseCase addDisputeMessage,
                             ResolveDisputeUseCase resolveDispute,
                             DisputeRepository disputeRepository) {
        this.createDispute = createDispute;
        this.addDisputeMessage = addDisputeMessage;
        this.resolveDispute = resolveDispute;
        this.disputeRepository = disputeRepository;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SHOP_AGENT')")
    public ResponseEntity<DisputeResponse> createDispute(@Valid @RequestBody CreateDisputeRequest request) {
        var current = CurrentUser.get();
        DisputeResponse response = createDispute.execute(request.orderId(), current.actor(), request.reason());
        return ResponseEntity.created(URI.create("/api/disputes/" + response.id())).body(response);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<DisputeResponse>> getDisputesByOrder(@PathVariable UUID orderId) {
        var current = CurrentUser.get();
        var disputes = disputeRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            UUID orgId = current.organizationId();
            if (orgId == null) {
                return ResponseEntity.status(403).build();
            }
            boolean allowed = disputes.isEmpty() || disputes.stream().anyMatch(d ->
                    orgId.equals(d.getShopId()) || orgId.equals(d.getSupplierId()));
            if (!allowed) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(disputes.stream().map(DisputeResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT', 'SYSTEM_ADMIN')")
    public ResponseEntity<DisputeResponse> getDispute(@PathVariable UUID id) {
        var dispute = disputeRepository.findById(id);
        if (dispute.isEmpty()) return ResponseEntity.notFound().build();
        var current = CurrentUser.get();
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            UUID orgId = current.organizationId();
            var d = dispute.get();
            if (orgId == null || (!orgId.equals(d.getShopId()) && !orgId.equals(d.getSupplierId()))) {
                return ResponseEntity.status(403).build();
            }
        }
        return ResponseEntity.ok(DisputeResponse.from(dispute.get()));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT')")
    public ResponseEntity<DisputeResponse> addMessage(
            @PathVariable UUID id,
            @Valid @RequestBody AddDisputeMessageRequest request) {
        var current = CurrentUser.get();
        DisputeResponse response = addDisputeMessage.execute(id, current.actor(), request.content());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        var current = CurrentUser.get();
        String targetStatus = body.getOrDefault("status", "RESOLVED");
        DisputeResponse response = resolveDispute.execute(id, current.actor(), targetStatus);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<DisputeResponse>> listDisputes(
            @RequestParam(required = false) String status) {
        var current = CurrentUser.get();
        boolean hasStatus = status != null && !status.isBlank();
        List<com.paymentplatform.organization.domain.model.Dispute> disputes;
        if (current.roles().contains("SYSTEM_ADMIN")) {
            disputes = hasStatus
                    ? disputeRepository.findByStatusOrderByCreatedAtDesc(status)
                    : disputeRepository.findAll();
        } else {
            UUID orgId = current.organizationId();
            if (orgId == null) {
                return ResponseEntity.status(403).build();
            }
            boolean supplierSide = current.roles().contains("SUPPLIER_ADMIN")
                    || current.roles().contains("SUPPLIER_AGENT");
            if (supplierSide) {
                disputes = hasStatus
                        ? disputeRepository.findBySupplierIdAndStatusOrderByCreatedAtDesc(orgId, status)
                        : disputeRepository.findBySupplierIdOrderByCreatedAtDesc(orgId);
            } else {
                disputes = hasStatus
                        ? disputeRepository.findByShopIdAndStatusOrderByCreatedAtDesc(orgId, status)
                        : disputeRepository.findByShopIdOrderByCreatedAtDesc(orgId);
            }
        }
        return ResponseEntity.ok(disputes.stream().map(DisputeResponse::from).toList());
    }
}
