package com.paymentplatform.payment.interfaces.rest;

import com.paymentplatform.payment.application.dto.*;
import com.paymentplatform.payment.application.service.PaymentInvoicePdfService;
import com.paymentplatform.payment.application.usecase.*;
import com.paymentplatform.payment.infrastructure.csv.CsvExportService;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.paymentplatform.payment.domain.model.PaymentStatus.*;

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
    private final OverduePaymentsUseCase overduePayments;
    private final CsvExportService csvExportService;
    private final PaymentInvoicePdfService paymentInvoicePdfService;

    public PaymentController(CreatePaymentUseCase createPayment,
                             GetPaymentUseCase getPayment,
                             ListPaymentsUseCase listPayments,
                             ConfirmPaymentUseCase confirmPayment,
                             RejectPaymentUseCase rejectPayment,
                             CancelPaymentUseCase cancelPayment,
                             AgentPaymentsBySupplierUseCase agentPayments,
                             OverduePaymentsUseCase overduePayments,
                             CsvExportService csvExportService,
                             PaymentInvoicePdfService paymentInvoicePdfService) {
        this.createPayment = createPayment;
        this.getPayment = getPayment;
        this.listPayments = listPayments;
        this.confirmPayment = confirmPayment;
        this.rejectPayment = rejectPayment;
        this.cancelPayment = cancelPayment;
        this.agentPayments = agentPayments;
        this.overduePayments = overduePayments;
        this.csvExportService = csvExportService;
        this.paymentInvoicePdfService = paymentInvoicePdfService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SHOP_CREATE_PAYMENTS')")
    public ResponseEntity<PaymentResponse> create(
            @Valid @RequestBody CreatePaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        var current = CurrentUser.get();
        return ResponseEntity.status(201).body(
                createPayment.execute(request, current.userId(), current.organizationId(), idempotencyKey));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('VIEW_PAYMENTS')")
    public ResponseEntity<PaymentResponse> getById(@PathVariable UUID id) {
        var current = CurrentUser.get();
        var response = getPayment.execute(id);
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            UUID orgId = current.organizationId();
            if (orgId == null) return ResponseEntity.status(403).build();
            boolean isShop = orgId.equals(response.shopId());
            boolean isSupplier = orgId.equals(response.supplierId());
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
            boolean isShop = orgId.equals(response.shopId());
            boolean isSupplier = orgId.equals(response.supplierId());
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

    @GetMapping("/overdue")
    @PreAuthorize("hasAuthority('VIEW_PAYMENTS')")
    public ResponseEntity<PageResponse<PaymentResponse>> overdue(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        var current = CurrentUser.get();
        String role = current.roles() != null && !current.roles().isEmpty()
                ? current.roles().getFirst() : null;
        return ResponseEntity.ok(overduePayments.execute(
                current.userId(), current.organizationId(), role, page, size));
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
            if (orgId == null || !orgId.equals(supplierId)) {
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

    @GetMapping("/supplier-summary")
    @PreAuthorize("hasAuthority('SUPPLIER_MANAGE_PAYMENTS')")
    public ResponseEntity<Map<String, Object>> supplierSummary(
            @RequestParam UUID supplierId) {
        var current = CurrentUser.get();
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            UUID orgId = current.organizationId();
            if (orgId == null || !orgId.equals(supplierId)) {
                return ResponseEntity.status(403).build();
            }
        }
        var payments = listPayments.executeBySupplier(supplierId, 0, Integer.MAX_VALUE);
        var items = payments.items();
        BigDecimal confirmedTotal = BigDecimal.ZERO;
        long confirmedCount = 0;
        BigDecimal pendingTotal = BigDecimal.ZERO;
        long pendingCount = 0;
        BigDecimal rejectedTotal = BigDecimal.ZERO;
        long rejectedCount = 0;
        for (var p : items) {
            switch (p.status()) {
                case CONFIRMED -> { confirmedTotal = confirmedTotal.add(p.amount()); confirmedCount++; }
                case PENDING -> { pendingTotal = pendingTotal.add(p.amount()); pendingCount++; }
                case REJECTED -> { rejectedTotal = rejectedTotal.add(p.amount()); rejectedCount++; }
                default -> {}
            }
        }
        return ResponseEntity.ok(Map.of(
                "confirmedTotal", confirmedTotal,
                "confirmedCount", confirmedCount,
                "pendingTotal", pendingTotal,
                "pendingCount", pendingCount,
                "rejectedTotal", rejectedTotal,
                "rejectedCount", rejectedCount
        ));
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

    @GetMapping("/export/csv")
    @PreAuthorize("hasAuthority('VIEW_PAYMENTS')")
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
            String role = current.roles().getFirst();
            if (role.contains("SHOP")) {
                shopId = current.organizationId();
            } else if (role.contains("SUPPLIER")) {
                supplierId = current.organizationId();
            }
        }

        if (dateFrom != null) {
            fromInstant = dateFrom.atStartOfDay().toInstant(ZoneOffset.UTC);
        }
        if (dateTo != null) {
            toInstant = dateTo.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC);
        }

        String csv;
        if (fromInstant != null && toInstant != null) {
            csv = csvExportService.generatePaymentsCsv(fromInstant, toInstant, supplierId, shopId);
        } else {
            Instant now = Instant.now();
            Instant yearAgo = now.minus(java.time.Duration.ofDays(365));
            csv = csvExportService.generatePaymentsCsv(yearAgo, now, supplierId, shopId);
        }

        if (status != null && !status.isBlank()) {
            String[] lines = csv.split("\n");
            StringBuilder filtered = new StringBuilder();
            if (lines.length > 0) {
                filtered.append(lines[0]).append("\n");
            }
            for (int i = 1; i < lines.length; i++) {
                String line = lines[i];
                String[] parts = line.split(",", -1);
                if (parts.length >= 6) {
                    String paymentStatus = parts[5].trim().replace("\"", "");
                    if (paymentStatus.equalsIgnoreCase(status)) {
                        filtered.append(line).append("\n");
                    }
                }
            }
            csv = filtered.toString();
        }

        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"payments.csv\"");
        response.getWriter().write(csv);
        response.getWriter().flush();
    }

    @GetMapping("/{id}/invoice")
    @PreAuthorize("hasAuthority('VIEW_PAYMENTS')")
    public void downloadInvoice(@PathVariable UUID id, HttpServletResponse httpResponse) throws Exception {
        var current = CurrentUser.get();
        var response = getPayment.execute(id);
        if (!current.roles().contains("SYSTEM_ADMIN")) {
            UUID orgId = current.organizationId();
            if (orgId == null) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès refusé");
                return;
            }
            boolean isShop = orgId.equals(response.shopId());
            boolean isSupplier = orgId.equals(response.supplierId());
            if (!isShop && !isSupplier) {
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, "Accès refusé");
                return;
            }
        }

        if (response.status() != CONFIRMED) {
            httpResponse.sendError(HttpServletResponse.SC_CONFLICT,
                    "La facture est disponible uniquement pour les paiements confirmés");
            return;
        }

        byte[] pdf = paymentInvoicePdfService.generateInvoicePdf(response);
        httpResponse.setContentType("application/pdf");
        httpResponse.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"facture-" + response.reference() + ".pdf\"");
        httpResponse.setContentLength(pdf.length);
        httpResponse.getOutputStream().write(pdf);
        httpResponse.getOutputStream().flush();
    }
}
