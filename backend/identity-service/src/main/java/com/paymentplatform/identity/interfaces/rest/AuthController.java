package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.identity.application.dto.*;
import com.paymentplatform.identity.application.usecase.*;
import com.paymentplatform.shared.domain.exception.UnauthorizedException;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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
    private final RefreshCookieSupport cookies;

    public AuthController(AuthUseCase auth, RegisterUseCase register, UserQueryUseCase users,
                          ChangePasswordUseCase changePassword, RefreshTokenService refreshTokens,
                          RefreshCookieSupport cookies) {
        this.auth = auth;
        this.register = register;
        this.users = users;
        this.changePassword = changePassword;
        this.refreshTokens = refreshTokens;
        this.cookies = cookies;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse res = auth.login(request);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.build(res.refreshToken(), res.refreshExpiresIn()).toString())
                .body(res);
    }

    /** Accepte le token en body (mobile Flutter) ou en cookie HttpOnly pp_rt (web PWA). */
    @PostMapping("/refresh")
    public ResponseEntity<RefreshResponse> refresh(@RequestBody(required = false) RefreshRequest request,
                                                   @CookieValue(name = RefreshCookieSupport.COOKIE_NAME,
                                                           required = false) String cookieToken) {
        String token = request != null && request.refreshToken() != null && !request.refreshToken().isBlank()
                ? request.refreshToken() : cookieToken;
        if (token == null || token.isBlank()) {
            throw new UnauthorizedException("Refresh token manquant");
        }
        RefreshResponse res = refreshTokens.refresh(token);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.build(res.refreshToken(), res.refreshExpiresIn()).toString())
                .body(res);
    }

    /** Accepte le token en body ou en cookie HttpOnly ; purge le cookie à la sortie. */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody(required = false) LogoutRequest request,
                                       @CookieValue(name = RefreshCookieSupport.COOKIE_NAME,
                                               required = false) String cookieToken) {
        String token = request != null && request.refreshToken() != null && !request.refreshToken().isBlank()
                ? request.refreshToken() : cookieToken;
        if (token != null && !token.isBlank()) {
            refreshTokens.revoke(token);
        }
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookies.clear().toString())
                .build();
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
