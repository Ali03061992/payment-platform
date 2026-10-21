package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.organization.application.dto.FiscalFeatureSettingsRequest;
import com.paymentplatform.organization.application.dto.OrganizationResponse;
import com.paymentplatform.organization.domain.model.Organization;
import com.paymentplatform.organization.domain.repository.OrganizationRepository;
import com.paymentplatform.organization.domain.valueobject.OrganizationId;
import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Reglages fiscaux du fournisseur connecte : activation des documents TEIF
 * (TTN) et des certificats de retenue TEJ.
 */
@RestController
@RequestMapping("/api/organizations/settings")
public class OrganizationSettingsController {

    private final OrganizationRepository organizations;

    public OrganizationSettingsController(OrganizationRepository organizations) {
        this.organizations = organizations;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_ADMIN')")
    public ResponseEntity<OrganizationResponse> getSettings() {
        return ResponseEntity.ok(OrganizationResponse.from(currentSupplier()));
    }

    @PatchMapping
    @PreAuthorize("hasAuthority('SUPPLIER_ADMIN')")
    public ResponseEntity<OrganizationResponse> updateSettings(
            @RequestBody FiscalFeatureSettingsRequest request) {
        Organization org = currentSupplier();
        org.updateFiscalFeatureToggles(
                request.teifEnabled() != null ? request.teifEnabled() : org.teifEnabled(),
                request.tejEnabled() != null ? request.tejEnabled() : org.tejEnabled());
        return ResponseEntity.ok(OrganizationResponse.from(organizations.save(org)));
    }

    private Organization currentSupplier() {
        var current = CurrentUser.get();
        if (current.organizationId() == null) {
            throw new ForbiddenException("Aucune organisation associee a cet utilisateur");
        }
        Organization org = organizations.findById(new OrganizationId(current.organizationId()))
                .orElseThrow(() -> new NotFoundException("Organisation non trouvee : "
                        + current.organizationId()));
        if (!org.isSupplier()) {
            throw new ForbiddenException("Les reglages fiscaux sont reserves aux fournisseurs");
        }
        return org;
    }
}
