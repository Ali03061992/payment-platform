package com.paymentplatform.notification.infrastructure.rest;

import com.paymentplatform.notification.application.NotificationPreferenceService;
import com.paymentplatform.notification.application.WebPushSubscriptionService;
import com.paymentplatform.notification.domain.model.NotificationPreference;
import com.paymentplatform.notification.domain.model.WebPushSubscription;
import com.paymentplatform.notification.infrastructure.push.VapidKeyProvider;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Inscription Web Push (PWA) et préférences de notification.
 * Authentification JWT requise — l'abonnement est toujours lié au user connecté.
 */
@RestController
@RequestMapping("/api/push")
public class WebPushController {

    private final WebPushSubscriptionService subscriptions;
    private final NotificationPreferenceService preferences;
    private final VapidKeyProvider vapidKeys;

    public WebPushController(WebPushSubscriptionService subscriptions,
                             NotificationPreferenceService preferences,
                             VapidKeyProvider vapidKeys) {
        this.subscriptions = subscriptions;
        this.preferences = preferences;
        this.vapidKeys = vapidKeys;
    }

    public record SubscribeRequest(
            @NotBlank @Size(max = 2048) String endpoint,
            @NotBlank String p256dh,
            @NotBlank String auth,
            @Size(max = 512) String userAgent) {
    }

    public record PreferenceRequest(boolean pushEnabled, boolean emailEnabled) {
    }

    /** Clé publique VAPID à injecter dans l'abonnement PushManager côté client. */
    @GetMapping("/vapid-public-key")
    public ResponseEntity<Map<String, String>> vapidPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", vapidKeys.publicKeyBase64Url()));
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<WebPushSubscription> subscribe(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody SubscribeRequest request) {
        return ResponseEntity.ok(subscriptions.register(
                user.userId(), user.organizationId(),
                request.endpoint(), request.p256dh(), request.auth(), request.userAgent()));
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<List<Map<String, Object>>> listSubscriptions(
            @AuthenticationPrincipal AuthenticatedUser user) {
        List<Map<String, Object>> result = subscriptions.list(user.userId()).stream()
                .map(sub -> Map.<String, Object>of(
                        "id", sub.id().toString(),
                        "endpoint", sub.endpoint(),
                        "userAgent", sub.userAgent() == null ? "" : sub.userAgent(),
                        "createdAt", sub.createdAt().toString(),
                        "lastUsedAt", sub.lastUsedAt() == null ? "" : sub.lastUsedAt().toString()))
                .toList();
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/subscriptions")
    public ResponseEntity<Void> unsubscribe(
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam String endpoint) {
        subscriptions.unregister(user.userId(), endpoint);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/subscriptions/{id}")
    public ResponseEntity<Void> unsubscribeById(
            @AuthenticationPrincipal AuthenticatedUser user,
            @PathVariable UUID id) {
        subscriptions.unregisterById(user.userId(), id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/preferences")
    public ResponseEntity<Map<String, Object>> getPreferences(
            @AuthenticationPrincipal AuthenticatedUser user) {
        NotificationPreference pref = preferences.getOrCreate(user.userId());
        return ResponseEntity.ok(Map.of(
                "pushEnabled", pref.pushEnabled(),
                "emailEnabled", pref.emailEnabled()));
    }

    @PutMapping("/preferences")
    public ResponseEntity<Map<String, Object>> updatePreferences(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody PreferenceRequest request) {
        NotificationPreference pref = preferences.update(
                user.userId(), request.pushEnabled(), request.emailEnabled());
        return ResponseEntity.ok(Map.of(
                "pushEnabled", pref.pushEnabled(),
                "emailEnabled", pref.emailEnabled()));
    }
}
