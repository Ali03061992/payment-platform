package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.CreateRelationRequest;
import com.paymentplatform.organization.application.dto.RelationResponse;
import com.paymentplatform.organization.application.usecase.SupplierShopRelationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/supplier-shop-relations")
public class SupplierShopRelationController {

    private final SupplierShopRelationUseCase relationUseCase;

    public SupplierShopRelationController(SupplierShopRelationUseCase relationUseCase) {
        this.relationUseCase = relationUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS')")
    public ResponseEntity<RelationResponse> create(@Valid @RequestBody CreateRelationRequest request) {
        return ResponseEntity.status(201).body(relationUseCase.createRelation(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS')")
    public ResponseEntity<List<RelationResponse>> list() {
        return ResponseEntity.ok(relationUseCase.listAll());
    }

    @GetMapping("/supplier/{supplierId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RelationResponse>> listBySupplier(@PathVariable UUID supplierId) {
        var current = com.paymentplatform.shared.infrastructure.security.CurrentUser.get();
        boolean isAdmin = current.roles().contains("SYSTEM_ADMIN");
        if (!isAdmin && current.organizationId() != null && !current.organizationId().equals(supplierId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(relationUseCase.listBySupplier(supplierId));
    }

    @GetMapping("/shop/{shopId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<RelationResponse>> listByShop(@PathVariable UUID shopId) {
        var current = com.paymentplatform.shared.infrastructure.security.CurrentUser.get();
        boolean isAdmin = current.roles().contains("SYSTEM_ADMIN");
        if (!isAdmin && current.organizationId() != null && !current.organizationId().equals(shopId)) {
            return ResponseEntity.status(403).build();
        }
        return ResponseEntity.ok(relationUseCase.listByShop(shopId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS')")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        relationUseCase.deactivateRelation(id);
        return ResponseEntity.noContent().build();
    }
}
