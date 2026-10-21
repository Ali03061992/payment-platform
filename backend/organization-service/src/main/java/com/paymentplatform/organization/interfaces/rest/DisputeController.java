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
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SHOP_MANAGER')")
    public ResponseEntity<DisputeResponse> createDispute(@Valid @RequestBody CreateDisputeRequest request) {
        var current = CurrentUser.get();
        String role = current.roles().contains("SUPPLIER_ADMIN") ? "SUPPLIER" : "SHOP";
        DisputeResponse response = createDispute.execute(request.orderId(), current.userId(), role, request.reason());
        return ResponseEntity.created(URI.create("/api/disputes/" + response.id())).body(response);
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<DisputeResponse>> getDisputesByOrder(@PathVariable UUID orderId) {
        var disputes = disputeRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        return ResponseEntity.ok(disputes.stream().map(DisputeResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER', 'SYSTEM_ADMIN')")
    public ResponseEntity<DisputeResponse> getDispute(@PathVariable UUID id) {
        var dispute = disputeRepository.findById(id);
        if (dispute.isEmpty()) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(DisputeResponse.from(dispute.get()));
    }

    @PostMapping("/{id}/messages")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_MANAGER')")
    public ResponseEntity<DisputeResponse> addMessage(
            @PathVariable UUID id,
            @Valid @RequestBody AddDisputeMessageRequest request) {
        var current = CurrentUser.get();
        String role = current.roles().contains("SUPPLIER_ADMIN") || current.roles().contains("SUPPLIER_AGENT")
                ? "SUPPLIER" : "SHOP";
        DisputeResponse response = addDisputeMessage.execute(id, current.userId(), role, request.content());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/resolve")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<DisputeResponse> resolveDispute(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body) {
        var current = CurrentUser.get();
        String targetStatus = body.getOrDefault("status", "RESOLVED");
        DisputeResponse response = resolveDispute.execute(id, current.userId(), targetStatus);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SHOP_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<DisputeResponse>> listDisputes(
            @RequestParam(required = false) String status) {
        var disputes = (status != null && !status.isBlank())
                ? disputeRepository.findByStatusOrderByCreatedAtDesc(status)
                : disputeRepository.findAll();
        return ResponseEntity.ok(disputes.stream().map(DisputeResponse::from).toList());
    }
}
