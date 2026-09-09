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
    private final com.paymentplatform.organization.domain.repository.SupplierShopRelationRepository relationRepository;

    public StockController(StockService stockService,
                          com.paymentplatform.organization.domain.repository.SupplierShopRelationRepository relationRepository) {
        this.stockService = stockService;
        this.relationRepository = relationRepository;
    }

    @GetMapping("/products")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ProductResponse>> listProducts(
            @PathVariable Long supplierId,
            @RequestParam(required = false) String status) {
        var current = CurrentUser.get();
        boolean isSupplierOwner = current.organizationId() != null && current.organizationId().equals(supplierId);
        boolean isSystemAdmin = current.roles().contains("SYSTEM_ADMIN");
        boolean hasSupplierAuthority = current.roles().contains("SUPPLIER_ADMIN") || current.roles().contains("SUPPLIER_AGENT") || isSystemAdmin;
        boolean hasShopAuthority = current.roles().contains("SHOP_ADMIN") || current.roles().contains("SHOP_AGENT");
        if (isSupplierOwner && hasSupplierAuthority) {
            return ResponseEntity.ok(stockService.listProducts(supplierId, status));
        }
        if (isSystemAdmin) {
            return ResponseEntity.ok(stockService.listProducts(supplierId, status));
        }
        if (hasShopAuthority && current.organizationId() != null) {
            boolean hasRelation = relationRepository.existsBySupplierIdAndShopIdAndStatus(
                    com.paymentplatform.organization.domain.valueobject.OrganizationId.of(supplierId),
                    com.paymentplatform.organization.domain.valueobject.OrganizationId.of(current.organizationId()),
                    com.paymentplatform.organization.domain.valueobject.RelationStatus.ACTIVE);
            if (hasRelation) {
                return ResponseEntity.ok(stockService.listProducts(supplierId, status));
            }
        }
        return ResponseEntity.status(403).build();
    }

    @GetMapping("/products/{productId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long supplierId, @PathVariable Long productId) {
        var current = CurrentUser.get();
        boolean isSupplierOwner = current.organizationId() != null && current.organizationId().equals(supplierId);
        boolean isSystemAdmin = current.roles().contains("SYSTEM_ADMIN");
        boolean hasSupplierAuthority = current.roles().contains("SUPPLIER_ADMIN") || current.roles().contains("SUPPLIER_AGENT") || isSystemAdmin;
        boolean hasShopAuthority = current.roles().contains("SHOP_ADMIN") || current.roles().contains("SHOP_AGENT");
        if (isSupplierOwner && hasSupplierAuthority) {
            return ResponseEntity.ok(stockService.getProduct(supplierId, productId));
        }
        if (isSystemAdmin) {
            return ResponseEntity.ok(stockService.getProduct(supplierId, productId));
        }
        if (hasShopAuthority && current.organizationId() != null) {
            boolean hasRelation = relationRepository.existsBySupplierIdAndShopIdAndStatus(
                    com.paymentplatform.organization.domain.valueobject.OrganizationId.of(supplierId),
                    com.paymentplatform.organization.domain.valueobject.OrganizationId.of(current.organizationId()),
                    com.paymentplatform.organization.domain.valueobject.RelationStatus.ACTIVE);
            if (hasRelation) {
                return ResponseEntity.ok(stockService.getProduct(supplierId, productId));
            }
        }
        return ResponseEntity.status(403).build();
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

    @PatchMapping("/products/{productId}/deactivate")
    @PreAuthorize("hasAnyAuthority('SUPPLIER_MANAGE_PRODUCTS','SYSTEM_ADMIN')")
    public ResponseEntity<ProductResponse> deleteProduct(@PathVariable Long supplierId, @PathVariable Long productId) {
        var current = CurrentUser.get();
        if (current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(stockService.deleteProduct(supplierId, productId));
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
