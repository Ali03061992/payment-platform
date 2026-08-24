package com.paymentplatform.organization.interfaces.rest;

import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.OrganizationResponse;
import com.paymentplatform.organization.application.dto.UpdateOrganizationRequest;
import com.paymentplatform.organization.application.usecase.CreateOrganizationUseCase;
import com.paymentplatform.organization.application.usecase.OrganizationQueryUseCase;
import com.paymentplatform.organization.application.usecase.OrganizationStatusUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminOrganizationController {

    private final CreateOrganizationUseCase createOrg;
    private final OrganizationQueryUseCase queryOrg;
    private final OrganizationStatusUseCase statusOrg;

    public AdminOrganizationController(CreateOrganizationUseCase createOrg,
                                       OrganizationQueryUseCase queryOrg,
                                       OrganizationStatusUseCase statusOrg) {
        this.createOrg = createOrg;
        this.queryOrg = queryOrg;
        this.statusOrg = statusOrg;
    }

    @PostMapping("/suppliers")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS')")
    public ResponseEntity<OrganizationResponse> createSupplier(
            @Valid @RequestBody CreateOrganizationRequest request) {
        var current = CurrentUser.get();
        return ResponseEntity.status(201).body(createOrg.execute(
                new CreateOrganizationRequest(request.name(), "SUPPLIER"), current.userId()));
    }

    @GetMapping("/suppliers")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS')")
    public ResponseEntity<List<OrganizationResponse>> listSuppliers() {
        return ResponseEntity.ok(queryOrg.listByType("SUPPLIER"));
    }

    @GetMapping("/suppliers/{id}")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS')")
    public ResponseEntity<OrganizationResponse> getSupplier(@PathVariable long id) {
        return ResponseEntity.ok(queryOrg.findById(id));
    }

    @PatchMapping("/suppliers/{id}/activate")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS')")
    public ResponseEntity<OrganizationResponse> activateSupplier(@PathVariable long id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(statusOrg.activate(id, current.userId()));
    }

    @PatchMapping("/suppliers/{id}/disable")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS')")
    public ResponseEntity<OrganizationResponse> disableSupplier(@PathVariable long id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(statusOrg.disable(id, current.userId()));
    }

    @PostMapping("/shops")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS')")
    public ResponseEntity<OrganizationResponse> createShop(
            @Valid @RequestBody CreateOrganizationRequest request) {
        var current = CurrentUser.get();
        return ResponseEntity.status(201).body(createOrg.execute(
                new CreateOrganizationRequest(request.name(), "SHOP"), current.userId()));
    }

    @GetMapping("/shops")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS')")
    public ResponseEntity<List<OrganizationResponse>> listShops() {
        return ResponseEntity.ok(queryOrg.listByType("SHOP"));
    }

    @GetMapping("/shops/{id}")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS')")
    public ResponseEntity<OrganizationResponse> getShop(@PathVariable long id) {
        return ResponseEntity.ok(queryOrg.findById(id));
    }

    @PatchMapping("/shops/{id}/activate")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS')")
    public ResponseEntity<OrganizationResponse> activateShop(@PathVariable long id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(statusOrg.activate(id, current.userId()));
    }

    @PatchMapping("/shops/{id}/disable")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_ORGANIZATIONS')")
    public ResponseEntity<OrganizationResponse> disableShop(@PathVariable long id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(statusOrg.disable(id, current.userId()));
    }

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('ADMIN_VIEW_STATS')")
    public ResponseEntity<Map<String, Long>> stats() {
        return ResponseEntity.ok(Map.of(
                "suppliers", queryOrg.countSuppliers(),
                "shops", queryOrg.countShops()));
    }
}
