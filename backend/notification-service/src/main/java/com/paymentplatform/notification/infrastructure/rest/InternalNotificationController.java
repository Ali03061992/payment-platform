package com.paymentplatform.notification.infrastructure.rest;

import com.paymentplatform.notification.infrastructure.messaging.NotificationBroadcaster;
import com.paymentplatform.shared.domain.security.InternalSecretValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Signal interne émis par l'API gateway après chaque mutation réussie :
 * tous les clients SSE connectés sont invités à revalider leurs données.
 * Le canal ne transporte aucune donnée métier, uniquement un événement vide.
 */
@RestController
@RequestMapping("/api/internal/notifications")
public class InternalNotificationController {

    private final NotificationBroadcaster broadcaster;
    private final String expectedSecret;

    public InternalNotificationController(NotificationBroadcaster broadcaster,
                                          @Value("${app.internal-secret}") String expectedSecret,
                                          Environment environment) {
        this.broadcaster = broadcaster;
        this.expectedSecret = InternalSecretValidator.requireValid(expectedSecret, environment);
    }

    @PostMapping("/data-changed")
    public ResponseEntity<Void> dataChanged(
            @RequestHeader(value = "X-Internal-Token", required = false) String token) {
        if (token == null || !token.equals(expectedSecret)) {
            return ResponseEntity.status(401).build();
        }
        broadcaster.broadcastDataChanged();
        return ResponseEntity.accepted().build();
    }
}
