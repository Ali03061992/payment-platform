package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import com.paymentplatform.identity.application.dto.LoginRequest;
import com.paymentplatform.identity.application.dto.LoginResponse;
import com.paymentplatform.identity.application.dto.UserResponse;
import com.paymentplatform.identity.application.usecase.AuthUseCase;
import com.paymentplatform.identity.application.usecase.UserQueryUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthUseCase auth;
    private final UserQueryUseCase users;

    public AuthController(AuthUseCase auth, UserQueryUseCase users) {
        this.auth = auth;
        this.users = users;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(auth.login(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        var current = CurrentUser.get();
        return ResponseEntity.ok(users.findById(current.userId(), current.userId(), current.roles(),
                current.organizationId()));
    }
}