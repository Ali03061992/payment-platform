package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.identity.application.dto.AgentCreatedResponse;
import com.paymentplatform.identity.application.dto.AgentRequest;
import com.paymentplatform.identity.application.dto.UpdateAgentRequest;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.application.usecase.AgentManagementUseCase;
import com.paymentplatform.shared.domain.model.RoleCode;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Gestion des agents d'un fournisseur : seule l'organisation propriétaire
 * peut gérer ses agents (périmètre vérifié côté backend, jamais l'UI seule).
 */
@RestController
@RequestMapping("/api/suppliers/{supplierId}/agents")
@PreAuthorize("hasAuthority('SUPPLIER_MANAGE_AGENTS')")
public class SupplierAgentController {

    private static final List<RoleCode> SUPPLIER_ROLES = List.of(RoleCode.SUPPLIER_ADMIN, RoleCode.SUPPLIER_AGENT);

    private final AgentManagementUseCase agents;

    public SupplierAgentController(AgentManagementUseCase agents) {
        this.agents = agents;
    }

    @GetMapping
    public ResponseEntity<List<UserResponse>> list(@PathVariable UUID supplierId) {
        return ResponseEntity.ok(agents.listAgents(CurrentUser.id(), supplierId));
    }

    @PostMapping
    public ResponseEntity<AgentCreatedResponse> create(@PathVariable UUID supplierId,
                                                       @Valid @RequestBody AgentRequest request) {
        AgentCreatedResponse created = agents.createAgent(CurrentUser.id(), supplierId, "SUPPLIER",
                SUPPLIER_ROLES, request);
        return ResponseEntity.status(201).body(created);
    }

    @PatchMapping("/{agentId}")
    public ResponseEntity<UserResponse> update(@PathVariable UUID supplierId, @PathVariable UUID agentId,
                                               @Valid @RequestBody UpdateAgentRequest request) {
        return ResponseEntity.ok(agents.updateAgent(CurrentUser.id(), supplierId, agentId, request));
    }

    @PatchMapping("/{agentId}/activate")
    public ResponseEntity<UserResponse> activate(@PathVariable UUID supplierId, @PathVariable UUID agentId) {
        return ResponseEntity.ok(agents.activateAgent(CurrentUser.id(), supplierId, agentId));
    }

    @PatchMapping("/{agentId}/disable")
    public ResponseEntity<UserResponse> disable(@PathVariable UUID supplierId, @PathVariable UUID agentId) {
        return ResponseEntity.ok(agents.disableAgent(CurrentUser.id(), supplierId, agentId));
    }
}