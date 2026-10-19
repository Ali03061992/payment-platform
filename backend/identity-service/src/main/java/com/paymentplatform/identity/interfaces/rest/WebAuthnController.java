package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.identity.application.dto.LoginResponse;
import com.paymentplatform.identity.application.dto.WebAuthnCredentialResponse;
import com.paymentplatform.identity.application.dto.WebAuthnLoginOptionsRequest;
import com.paymentplatform.identity.application.dto.WebAuthnLoginRequest;
import com.paymentplatform.identity.application.dto.WebAuthnRegisterRequest;
import com.paymentplatform.identity.application.dto.WebAuthnRenameRequest;
import com.paymentplatform.identity.application.usecase.WebAuthnCredentialManagementUseCase;
import com.paymentplatform.identity.application.usecase.WebAuthnLoginUseCase;
import com.paymentplatform.identity.application.usecase.WebAuthnRegistrationUseCase;
import com.paymentplatform.shared.infrastructure.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialCreationOptions;
import org.springframework.security.web.webauthn.api.PublicKeyCredentialRequestOptions;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Endpoints WebAuthn / Passkeys.
 * Les routes /login et /login/options sont publiques (rate-limitées au gateway) ;
 * l'enregistrement et la gestion des appareils exigent une session JWT valide.
 */
@RestController
@RequestMapping("/api/auth/webauthn")
public class WebAuthnController {

    private final WebAuthnRegistrationUseCase registration;
    private final WebAuthnLoginUseCase login;
    private final WebAuthnCredentialManagementUseCase management;
    private final RefreshCookieSupport cookies;

    public WebAuthnController(WebAuthnRegistrationUseCase registration,
                              WebAuthnLoginUseCase login,
                              WebAuthnCredentialManagementUseCase management,
                              RefreshCookieSupport cookies) {
        this.registration = registration;
        this.login = login;
        this.management = management;
        this.cookies = cookies;
    }

    @PostMapping("/register/options")
    public ResponseEntity<PublicKeyCredentialCreationOptions> registerOptions() {
        return ResponseEntity.ok(registration.startRegistration(CurrentUser.id()));
    }

    @PostMapping("/register")
    public ResponseEntity<WebAuthnCredentialResponse> register(@Valid @RequestBody WebAuthnRegisterRequest request) {
        return ResponseEntity.status(201).body(
                registration.finishRegistration(CurrentUser.id(), request.credential(), request.label()));
    }

    @GetMapping("/credentials")
    public ResponseEntity<List<WebAuthnCredentialResponse>> listCredentials() {
        return ResponseEntity.ok(management.list(CurrentUser.id()));
    }

    @PatchMapping("/credentials/{id}")
    public ResponseEntity<Void> renameCredential(@PathVariable UUID id,
                                                 @Valid @RequestBody WebAuthnRenameRequest request) {
        management.rename(CurrentUser.id(), id, request.label());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/credentials/{id}")
    public ResponseEntity<Void> revokeCredential(@PathVariable UUID id) {
        management.revoke(CurrentUser.id(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/login/options")
    public ResponseEntity<PublicKeyCredentialRequestOptions> loginOptions(
            @RequestBody(required = false) WebAuthnLoginOptionsRequest request) {
        return ResponseEntity.ok(login.startLogin(request == null ? null : request.username()));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody WebAuthnLoginRequest request) {
        LoginResponse res = login.finishLogin(request.credential());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookies.build(res.refreshToken(), res.refreshExpiresIn()).toString())
                .body(res);
    }
}
