package com.paymentplatform.payment.interfaces.rest;

import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import com.paymentplatform.payment.application.dto.*;
import com.paymentplatform.payment.application.usecase.*;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final CreatePaymentUseCase createPayment;
    private final GetPaymentUseCase getPayment;
    private final ListPaymentsUseCase listPayments;
    private final ConfirmPaymentUseCase confirmPayment;
    private final RejectPaymentUseCase rejectPayment;
    private final CancelPaymentUseCase cancelPayment;
    private final AgentPaymentsBySupplierUseCase agentPayments;
    private final SearchPaymentsUseCase searchPayments;

    public PaymentController(CreatePaymentUseCase createPayment,
                             GetPaymentUseCase getPayment,
                             ListPaymentsUseCase listPayments,
                             ConfirmPaymentUseCase confirmPayment,
                             RejectPaymentUseCase rejectPayment,
                             CancelPaymentUseCase cancelPayment,
                             AgentPaymentsBySupplierUseCase agentPayments,
                             SearchPaymentsUseCase searchPayments) {
        this.createPayment = createPayment;
        this.getPayment = getPayment;
        this.listPayments = listPayments;
        this.confirmPayment = confirmPayment;
        this.rejectPayment = rejectPayment;
        this.cancelPayment = cancelPayment;
        this.agentPayments = agentPayments;
        this.searchPayments = searchPayments;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SHOP_CREATE_PAYMENTS')")
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        var current = CurrentUser.get();
        return ResponseEntity.status(201).body(
                createPayment.execute(request, current.userId(), current.organizationId()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_PAYMENTS')")
    public ResponseEntity<PaymentResponse> getById(@PathVariable long id) {
        var current = CurrentUser.get();
        var response = getPayment.execute(id);
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            Long orgId = current.organizationId();
            if (orgId == null) return ResponseEntity.status(403).build();
            boolean isShop = response.shopId() == orgId;
            boolean isSupplier = response.supplierId() == orgId;
            if (!isShop && !isSupplier) return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reference/{reference}")
    @PreAuthorize("hasAuthority('VIEW_PAYMENTS')")
    public ResponseEntity<PaymentResponse> getByReference(@PathVariable String reference) {
        return ResponseEntity.ok(getPayment.execute(reference));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_PAYMENTS')")
    public ResponseEntity<List<PaymentResponse>> list() {
        var current = CurrentUser.get();
        if (current.organizationId() != null) {
            String role = current.roles().getFirst();
            if (role.contains("SHOP")) {
                return ResponseEntity.ok(listPayments.execute(current.organizationId()));
            } else if (role.contains("SUPPLIER")) {
                return ResponseEntity.ok(listPayments.executeBySupplier(current.organizationId()));
            }
        }
        return ResponseEntity.ok(listPayments.executeAll());
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('VIEW_PAYMENTS')")
    public ResponseEntity<PaymentStatsResponse> stats() {
        return ResponseEntity.ok(listPayments.stats());
    }

    @GetMapping("/agent-summary")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<AgentPaymentSummary>> agentSummary(
            @RequestParam long supplierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var current = CurrentUser.get();
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            Long orgId = current.organizationId();
            if (orgId == null || orgId != supplierId) {
                return ResponseEntity.status(403).build();
            }
        }
        Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        return ResponseEntity.ok(agentPayments.execute(supplierId, fromInstant, toInstant));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('SUPPLIER_MANAGE_PAYMENTS')")
    public ResponseEntity<PaymentResponse> confirm(@PathVariable long id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(confirmPayment.execute(id, current.userId(), current.organizationId()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('SUPPLIER_MANAGE_PAYMENTS')")
    public ResponseEntity<PaymentResponse> reject(
            @PathVariable long id,
            @Valid @RequestBody RejectPaymentRequest request) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(rejectPayment.execute(id, request, current.userId(), current.organizationId()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('SHOP_CANCEL_PAYMENTS', 'SUPPLIER_MANAGE_PAYMENTS')")
    public ResponseEntity<PaymentResponse> cancel(@PathVariable long id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(cancelPayment.execute(id, current.userId(), current.organizationId()));
    }

    @PostMapping("/search")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<SearchPaymentsResponse> search(
            @RequestBody SearchPaymentsRequest request) {
        var current = CurrentUser.get();
        Long supplierId = null;
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            supplierId = current.organizationId();
        }
        return ResponseEntity.ok(searchPayments.execute(request, supplierId));
    }
}
