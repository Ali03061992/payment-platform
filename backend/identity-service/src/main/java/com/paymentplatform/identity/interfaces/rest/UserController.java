package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.application.usecase.UserQueryUseCase;
import com.paymentplatform.identity.application.usecase.UserStatusUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserQueryUseCase query;
    private final UserStatusUseCase status;

    public UserController(UserQueryUseCase query, UserStatusUseCase status) {
        this.query = query;
        this.status = status;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_USERS')")
    public ResponseEntity<List<UserResponse>> list(@RequestParam(required = false) Long organizationId,
                                                   @RequestParam(required = false) String role,
                                                   @RequestParam(required = false) String statusFilter) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(query.list(current.userId(), current.roles(), current.organizationId(),
                organizationId, role, statusFilter));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> findById(@PathVariable long id) {
        var current = CurrentUser.get();
        return ResponseEntity.ok(query.findById(current.userId(), id, current.roles(), current.organizationId()));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_USERS')")
    public ResponseEntity<UserResponse> activate(@PathVariable long id) {
        return ResponseEntity.ok(status.activateUser(CurrentUser.id(), id));
    }

    @PatchMapping("/{id}/disable")
    @PreAuthorize("hasAuthority('ADMIN_MANAGE_USERS')")
    public ResponseEntity<UserResponse> disable(@PathVariable long id) {
        return ResponseEntity.ok(status.disableUser(CurrentUser.id(), id));
    }
}