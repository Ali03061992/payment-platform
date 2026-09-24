package com.paymentplatform.notification.infrastructure.rest;

import com.paymentplatform.notification.domain.model.FcmToken;
import com.paymentplatform.notification.domain.model.FcmTokenRepository;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/fcm-tokens")
public class FcmTokenController {

    private static final Logger log = LoggerFactory.getLogger(FcmTokenController.class);

    private final FcmTokenRepository fcmTokenRepository;

    public FcmTokenController(FcmTokenRepository fcmTokenRepository) {
        this.fcmTokenRepository = fcmTokenRepository;
    }

    @PostMapping
    public ResponseEntity<Void> registerToken(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, String> body) {

        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        Optional<FcmToken> existing = fcmTokenRepository.findByToken(token);
        if (existing.isPresent()) {
            existing.get().touch();
            fcmTokenRepository.save(existing.get());
            return ResponseEntity.ok().build();
        }

        FcmToken fcmToken = new FcmToken(user.userId(), token);
        fcmTokenRepository.save(fcmToken);
        log.info("FCM token registered for user {}", user.userId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> unregisterToken(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestBody Map<String, String> body) {

        String token = body.get("token");
        if (token == null || token.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        fcmTokenRepository.deleteByToken(token);
        log.info("FCM token unregistered for user {}", user.userId());
        return ResponseEntity.ok().build();
    }
}
