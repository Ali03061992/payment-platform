package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.OrganizationStatusResponse;
import com.paymentplatform.organization.application.usecase.OrganizationValidationUseCase;
import com.paymentplatform.organization.domain.model.SupplierShopRelation;
import com.paymentplatform.organization.domain.repository.SupplierShopRelationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/organizations/internal")
public class InternalOrganizationController {

    private final OrganizationValidationUseCase validation;
    private final SupplierShopRelationRepository relations;

    @Value("${app.internal-secret:dev-internal-secret-change-me}")
    private String expectedSecret;

    public InternalOrganizationController(OrganizationValidationUseCase validation,
                                           SupplierShopRelationRepository relations) {
        this.validation = validation;
        this.relations = relations;
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<OrganizationStatusResponse> getStatus(
            @PathVariable long id,
            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (token == null || !token.equals(expectedSecret)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(validation.validate(id));
    }

    @GetMapping("/relations/supplier/{supplierId}")
    public ResponseEntity<List<Map<String, Object>>> getRelationsBySupplier(
            @PathVariable long supplierId,
            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (token == null || !token.equals(expectedSecret)) {
            return ResponseEntity.status(401).build();
        }
        OrganizationId orgId = new OrganizationId(supplierId);
        List<SupplierShopRelation> list = relations.findBySupplierId(orgId);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SupplierShopRelation r : list) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", r.id());
            map.put("supplierId", r.supplierId().value());
            map.put("shopId", r.shopId().value());
            map.put("status", r.status().name());
            result.add(map);
        }
        return ResponseEntity.ok(result);
    }
}
