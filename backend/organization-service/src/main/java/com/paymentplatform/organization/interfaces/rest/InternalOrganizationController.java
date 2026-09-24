package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.OrganizationStatusResponse;
import com.paymentplatform.organization.application.usecase.OrganizationValidationUseCase;
import com.paymentplatform.organization.domain.model.SupplierShopRelation;
import com.paymentplatform.organization.domain.repository.SupplierShopRelationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.shared.domain.security.InternalSecretValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/organizations/internal")
public class InternalOrganizationController {

    private final OrganizationValidationUseCase validation;
    private final SupplierShopRelationRepository relations;
    private final String expectedSecret;

    public InternalOrganizationController(OrganizationValidationUseCase validation,
                                           SupplierShopRelationRepository relations,
                                           @Value("${app.internal-secret}") String expectedSecret,
                                           Environment environment) {
        this.validation = validation;
        this.relations = relations;
        // B3 : échec au boot si absent ; refus des défauts connus sous profil prod.
        this.expectedSecret = InternalSecretValidator.requireValid(expectedSecret, environment);
    }

    @GetMapping("/{id}/status")
    public ResponseEntity<OrganizationStatusResponse> getStatus(
            @PathVariable UUID id,
            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (token == null || !token.equals(expectedSecret)) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok(validation.validate(id));
    }

    @GetMapping("/relations/supplier/{supplierId}")
    public ResponseEntity<List<Map<String, Object>>> getRelationsBySupplier(
            @PathVariable UUID supplierId,
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
