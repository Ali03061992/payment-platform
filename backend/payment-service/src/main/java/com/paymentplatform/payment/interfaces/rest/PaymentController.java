package com.paymentplatform.payment.interfaces.rest;

import java.util.UUID;

import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import com.paymentplatform.payment.application.dto.*;
import com.paymentplatform.payment.application.usecase.*;
import com.paymentplatform.payment.infrastructure.csv.CsvExportService;
import com.paymentplatform.payment.infrastructure.elasticsearch.PaymentIndexerService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
    private final PaymentIndexerService indexerService;
    private final CsvExportService csvExportService;

    public PaymentController(CreatePaymentUseCase createPayment,
                             GetPaymentUseCase getPayment,
                             ListPaymentsUseCase listPayments,
                             ConfirmPaymentUseCase confirmPayment,
                             RejectPaymentUseCase rejectPayment,
                             CancelPaymentUseCase cancelPayment,
                             AgentPaymentsBySupplierUseCase agentPayments,
                             SearchPaymentsUseCase searchPayments,
                             PaymentIndexerService indexerService,
                             CsvExportService csvExportService) {
        this.createPayment = createPayment;
        this.getPayment = getPayment;
        this.listPayments = listPayments;
        this.confirmPayment = confirmPayment;
        this.rejectPayment = rejectPayment;
        this.cancelPayment = cancelPayment;
        this.agentPayments = agentPayments;
        this.searchPayments = searchPayments;
        this.indexerService = indexerService;
        this.csvExportService = csvExportService;
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
    public ResponseEntity<PaymentResponse> getById(@PathVariable UUID id) {
        var current = CurrentUser.get();
        var response = getPayment.execute(id);
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            UUID orgId = current.organizationId();
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
        var current = CurrentUser.get();
        var response = getPayment.execute(reference);
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            UUID orgId = current.organizationId();
            if (orgId == null) return ResponseEntity.status(403).build();
            boolean isShop = response.shopId() == orgId;
            boolean isSupplier = response.supplierId() == orgId;
            if (!isShop && !isSupplier) return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VIEW_PAYMENTS')")
    public ResponseEntity<PageResponse<PaymentResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var current = CurrentUser.get();
        if (current.organizationId() != null) {
            String role = current.roles().getFirst();
            if (role.contains("SHOP")) {
                return ResponseEntity.ok(listPayments.execute(current.organizationId(), page, size));
            } else if (role.contains("SUPPLIER")) {
                return ResponseEntity.ok(listPayments.executeBySupplier(current.organizationId(), page, size));
            }
        }
        return ResponseEntity.ok(listPayments.executeAll(page, size));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('VIEW_PAYMENTS')")
    public ResponseEntity<PaymentStatsResponse> stats() {
        return ResponseEntity.ok(listPayments.stats());
    }

    @GetMapping("/agent-summary")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<List<AgentPaymentSummary>> agentSummary(
            @RequestParam UUID supplierId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        var current = CurrentUser.get();
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            UUID orgId = current.organizationId();
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
    public ResponseEntity<PaymentResponse> confirm(@PathVariable UUID id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(confirmPayment.execute(id, current.userId(), current.organizationId()));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('SUPPLIER_MANAGE_PAYMENTS')")
    public ResponseEntity<PaymentResponse> reject(
            @PathVariable UUID id,
            @Valid @RequestBody RejectPaymentRequest request) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(rejectPayment.execute(id, request, current.userId(), current.organizationId()));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyAuthority('SHOP_CANCEL_PAYMENTS', 'SUPPLIER_MANAGE_PAYMENTS')")
    public ResponseEntity<PaymentResponse> cancel(@PathVariable UUID id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(cancelPayment.execute(id, current.userId(), current.organizationId()));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAuthority('VIEW_PAYMENTS')")
    public void exportCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            HttpServletResponse response) throws Exception {
        var current = CurrentUser.get();
        UUID supplierId = null;
        UUID shopId = null;
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            String role = current.roles().getFirst();
            if (role.contains("SHOP")) {
                shopId = current.organizationId();
            } else if (role.contains("SUPPLIER")) {
                supplierId = current.organizationId();
            }
        }
        Instant fromInstant = from.atStartOfDay().toInstant(ZoneOffset.UTC);
        Instant toInstant = to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        String csv = csvExportService.generatePaymentsCsv(fromInstant, toInstant, supplierId, shopId);
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payments.csv\"");
        response.getWriter().write(csv);
        response.getWriter().flush();
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasAuthority('SYSTEM_ADMIN')")
    public ResponseEntity<Map<String, Object>> reindex() {
        int count = indexerService.reindexAll();
        return ResponseEntity.ok(Map.of("indexed", count));
    }
}
