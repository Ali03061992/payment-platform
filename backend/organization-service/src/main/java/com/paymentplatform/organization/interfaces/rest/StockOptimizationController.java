package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.StockOptimizationResponse;
import com.paymentplatform.organization.application.service.StockOptimizationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/suppliers/{supplierId}/optimization")
public class StockOptimizationController {

    private final StockOptimizationService optimizationService;

    public StockOptimizationController(StockOptimizationService optimizationService) {
        this.optimizationService = optimizationService;
    }

    /**
     * Run full stock optimization for a supplier.
     * Returns ABC/XYZ classification, forecasts, safety stock, reorder points,
     * EOQ, risk analysis, anomalies, and recommendations for every product.
     */
    @GetMapping
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SYSTEM_ADMIN')")
    public ResponseEntity<StockOptimizationResponse> optimize(@PathVariable UUID supplierId) {
        StockOptimizationResponse result = optimizationService.optimize(supplierId);
        return ResponseEntity.ok(result);
    }

    /**
     * Update optimization parameters and re-run.
     */
    @PostMapping("/configure")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_ADMIN', 'SYSTEM_ADMIN')")
    public ResponseEntity<StockOptimizationResponse> configure(
            @PathVariable UUID supplierId,
            @RequestParam(defaultValue = "7") double leadTimeDays,
            @RequestParam(defaultValue = "50") double orderingCost,
            @RequestParam(defaultValue = "0.25") double holdingCostPercent) {
        optimizationService.setParameters(leadTimeDays, orderingCost, holdingCostPercent);
        StockOptimizationResponse result = optimizationService.optimize(supplierId);
        return ResponseEntity.ok(result);
    }
}
