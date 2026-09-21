package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.identity.application.dto.CreateInternalUserRequest;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.application.usecase.InternalUserCreationUseCase;
import com.paymentplatform.identity.application.usecase.UserQueryUseCase;
import com.paymentplatform.identity.application.usecase.UserStatusUseCase;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserQueryUseCase query;
    private final UserStatusUseCase status;
    private final InternalUserCreationUseCase creation;

    public UserController(UserQueryUseCase query, UserStatusUseCase status, InternalUserCreationUseCase creation) {
        this.query = query;
        this.status = status;
        this.creation = creation;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_USERS')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateInternalUserRequest request) {
        return ResponseEntity.status(201).body(creation.createInternalUser(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_USERS')")
    public ResponseEntity<List<UserResponse>> list(@RequestParam(required = false) UUID organizationId,
                                                   @RequestParam(required = false) String role,
                                                   @RequestParam(required = false) String statusFilter) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(query.list(current.userId(), current.roles(), current.organizationId(),
                organizationId, role, statusFilter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(query.findById(current.userId(), id, current.roles(), current.organizationId()));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_USERS')")
    public ResponseEntity<UserResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(status.activateUser(CurrentUser.id(), id));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_USERS')")
    public ResponseEntity<UserResponse> disable(@PathVariable UUID id) {
        return ResponseEntity.ok(status.disableUser(CurrentUser.id(), id));
    }
}