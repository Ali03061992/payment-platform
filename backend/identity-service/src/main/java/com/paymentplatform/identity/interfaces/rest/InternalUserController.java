package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.shared.domain.exception.ForbiddenException;
import com.paymentplatform.identity.application.dto.CreateInternalUserRequest;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.application.usecase.InternalUserCreationUseCase;
import com.paymentplatform.identity.application.usecase.UserQueryUseCase;
import com.paymentplatform.identity.infrastructure.http.InternalAuthGuard;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Endpoint interne (secret partagé) utilisé par Organization Service. */
@RestController
@RequestMapping("/api/internal/users")
public class InternalUserController {

    private final InternalUserCreationUseCase useCase;
    private final UserQueryUseCase query;
    private final InternalAuthGuard guard;

    public InternalUserController(InternalUserCreationUseCase useCase,
                                   UserQueryUseCase query,
                                   InternalAuthGuard guard) {
        this.useCase = useCase;
        this.query = query;
        this.guard = guard;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(@RequestHeader("X-Internal-Token") String token,
                                               @Valid @RequestBody CreateInternalUserRequest request) {
        if (!guard.isValid(token)) {
            throw new ForbiddenException("Secret interne invalide");
        }
        return ResponseEntity.status(201).body(useCase.createInternalUser(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponse> getById(@RequestHeader("X-Internal-Token") String token,
                                                @PathVariable long id) {
        if (!guard.isValid(token)) {
            throw new ForbiddenException("Secret interne invalide");
        }
        return ResponseEntity.ok(query.findByIdInternal(id));
    }
}