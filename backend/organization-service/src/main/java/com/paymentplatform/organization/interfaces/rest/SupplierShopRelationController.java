package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import com.paymentplatform.organization.application.dto.CreateRelationRequest;
import com.paymentplatform.organization.application.dto.RelationResponse;
import com.paymentplatform.organization.application.usecase.SupplierShopRelationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS') or hasAuthority('SUPPLIER_MANAGE_AGENTS')")
    public ResponseEntity<List<RelationResponse>> listBySupplier(@PathVariable long supplierId) {
        return ResponseEntity.ok(relationUseCase.listBySupplier(supplierId));
    }

    @GetMapping("/shop/{shopId}")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS') or hasAuthority('SHOP_MANAGE_AGENTS')")
    public ResponseEntity<List<RelationResponse>> listByShop(@PathVariable long shopId) {
        return ResponseEntity.ok(relationUseCase.listByShop(shopId));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS')")
    public ResponseEntity<Void> deactivate(@PathVariable long id) {
        relationUseCase.deactivateRelation(id);
        return ResponseEntity.noContent().build();
    }
}
