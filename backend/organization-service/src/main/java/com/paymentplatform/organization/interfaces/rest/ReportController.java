package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.SupplierFinancialReportResponse;
import com.paymentplatform.organization.application.usecase.GetSupplierFinancialReportUseCase;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final GetSupplierFinancialReportUseCase getSupplierFinancialReport;

    public ReportController(GetSupplierFinancialReportUseCase getSupplierFinancialReport) {
        this.getSupplierFinancialReport = getSupplierFinancialReport;
    }

    @GetMapping("/supplier-financial")
    @PreAuthorize("hasAuthority('SUPPLIER_ADMIN')")
    public ResponseEntity<SupplierFinancialReportResponse> supplierFinancial() {
        var current = CurrentUser.get();
        if (current.organizationId() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(getSupplierFinancialReport.execute(current.organizationId()));
    }
}
