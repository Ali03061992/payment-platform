package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.identity.application.dto.CompletePasswordSetupRequest;
import com.paymentplatform.identity.application.usecase.PasswordSetupUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/password-setup")
public class PasswordSetupController {

    private final PasswordSetupUseCase passwordSetup;

    public PasswordSetupController(PasswordSetupUseCase passwordSetup) {
        this.passwordSetup = passwordSetup;
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestParam String token) {
        boolean valid = passwordSetup.isValidToken(token);
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    @PostMapping("/complete")
    public ResponseEntity<Map<String, String>> complete(@Valid @RequestBody CompletePasswordSetupRequest request) {
        passwordSetup.completeSetup(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Mot de passe configuré avec succès"));
    }
}
