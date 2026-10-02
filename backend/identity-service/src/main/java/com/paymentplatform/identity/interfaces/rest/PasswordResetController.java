package com.paymentplatform.identity.interfaces.rest;

import com.paymentplatform.identity.application.dto.ForgotPasswordRequest;
import com.paymentplatform.identity.application.dto.ResetPasswordRequest;
import com.paymentplatform.identity.application.usecase.PasswordResetUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth/password-reset")
public class PasswordResetController {

    private final PasswordResetUseCase passwordReset;

    public PasswordResetController(PasswordResetUseCase passwordReset) {
        this.passwordReset = passwordReset;
    }

    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> request(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordReset.requestReset(request.email());
        return ResponseEntity.ok(Map.of(
                "message", "Un lien de réinitialisation a été envoyé à votre adresse email"));
    }

    @GetMapping("/validate")
    public ResponseEntity<Map<String, Boolean>> validateToken(@RequestParam String token) {
        return ResponseEntity.ok(Map.of("valid", passwordReset.isValidToken(token)));
    }

    @PostMapping("/confirm")
    public ResponseEntity<Map<String, String>> confirm(@Valid @RequestBody ResetPasswordRequest request) {
        passwordReset.confirmReset(request.token(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Mot de passe réinitialisé avec succès"));
    }
}
