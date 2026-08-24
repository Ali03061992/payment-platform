package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.OrganizationStatusResponse;
import com.paymentplatform.organization.application.usecase.OrganizationValidationUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organizations/internal")
public class InternalOrganizationController {

    private final OrganizationValidationUseCase validation;

    @Value("${app.internal-secret:dev-internal-secret-change-me}")
    private String expectedSecret;

    public InternalOrganizationController(OrganizationValidationUseCase validation) {
        this.validation = validation;
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
}
