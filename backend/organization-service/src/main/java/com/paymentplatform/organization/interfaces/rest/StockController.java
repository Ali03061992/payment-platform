package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.*;
import com.paymentplatform.organization.application.service.StockService;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers/{supplierId}")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/products")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_MANAGE_PRODUCTS','SUPPLIER_MANAGE_STOCK','SYSTEM_ADMIN')")
    public ResponseEntity<List<ProductResponse>> listProducts(
            @PathVariable Long supplierId,
            @RequestParam(required = false) String status) {
        var current = CurrentUser.get();
        if (current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(stockService.listProducts(supplierId, status));
    }

    @GetMapping("/products/{productId}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_MANAGE_PRODUCTS','SUPPLIER_MANAGE_STOCK','SYSTEM_ADMIN')")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long supplierId, @PathVariable Long productId) {
        var current = CurrentUser.get();
        if (current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(stockService.getProduct(supplierId, productId));
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_MANAGE_PRODUCTS','SYSTEM_ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(
            @PathVariable Long supplierId,
            @Valid @RequestBody ProductCreateRequest request) {
        var current = CurrentUser.get();
        if (current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(stockService.createProduct(supplierId, request));
    }

    @PatchMapping("/products/{productId}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_MANAGE_PRODUCTS','SYSTEM_ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable Long supplierId,
            @PathVariable Long productId,
            @RequestBody ProductUpdateRequest request) {
        var current = CurrentUser.get();
        if (current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(stockService.updateProduct(supplierId, productId, request));
    }

    @DeleteMapping("/products/{productId}")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_MANAGE_PRODUCTS','SYSTEM_ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable Long supplierId, @PathVariable Long productId) {
        var current = CurrentUser.get();
        if (current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        stockService.deleteProduct(supplierId, productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_MANAGE_STOCK','SYSTEM_ADMIN')")
    public ResponseEntity<List<StockMovementResponse>> listMovements(
            @PathVariable Long supplierId,
            @RequestParam(required = false) Long productId) {
        var current = CurrentUser.get();
        if (current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(stockService.listMovements(supplierId, productId));
    }

    @PostMapping("/movements")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_MANAGE_STOCK','SYSTEM_ADMIN')")
    public ResponseEntity<StockMovementResponse> createMovement(
            @PathVariable Long supplierId,
            @Valid @RequestBody StockMovementRequest request) {
        var current = CurrentUser.get();
        if (current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(stockService.createMovement(supplierId, request));
    }
}
