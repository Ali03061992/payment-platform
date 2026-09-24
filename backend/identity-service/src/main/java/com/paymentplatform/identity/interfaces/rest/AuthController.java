package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.identity.application.dto.*;
import com.paymentplatform.identity.application.usecase.AuthUseCase;
import com.paymentplatform.identity.application.usecase.ChangePasswordUseCase;
import com.paymentplatform.identity.application.usecase.RefreshTokenService;
import com.paymentplatform.identity.application.usecase.RegisterUseCase;
import com.paymentplatform.identity.application.usecase.UserQueryUseCase;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthUseCase auth;
    private final RegisterUseCase register;
    private final UserQueryUseCase users;
    private final ChangePasswordUseCase changePassword;
    private final RefreshTokenService refreshTokens;

    public AuthController(AuthUseCase auth, RegisterUseCase register, UserQueryUseCase users,
                          ChangePasswordUseCase changePassword, RefreshTokenService refreshTokens) {
        this.auth = auth;
        this.register = register;
        this.users = users;
        this.changePassword = changePassword;
        this.refreshTokens = refreshTokens;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(auth.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(refreshTokens.refresh(request.refreshToken()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request) {
        refreshTokens.revoke(request.refreshToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(201).body(register.register(request));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> me() {
        var current = CurrentUser.get();
        return ResponseEntity.ok(users.findById(current.userId(), current.userId(), current.roles(),
                current.organizationId()));
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        changePassword.changePassword(request);
        return ResponseEntity.noContent().build();
    }
}