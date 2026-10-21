package com.paymentplatform.notification.infrastructure.push;

import com.paymentplatform.notification.application.NotificationPreferenceService;
import com.paymentplatform.notification.application.WebPushSubscriptionService;
import com.paymentplatform.notification.domain.model.WebPushSubscription;
import nl.martijndwars.webpush.Notification;
import nl.martijndwars.webpush.PushService;
import org.apache.http.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
 * Envoi Web Push vers les PWA inscrites. Contrat payload compatible ngsw-worker :
 * le message contient une clé "notification" (title/body/icon/tag/data.onActionClick)
 * que le service worker Angular transforme en Notification système + deep-link au clic.
 * Nettoie automatiquement les endpoints expirés (404/410) et les URLs dangereuses.
 */
@Service
public class WebPushSender {

    private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);

    private final WebPushSubscriptionService subscriptions;
    private final NotificationPreferenceService preferences;
    private final VapidKeyProvider vapidKeys;
    private final boolean enabled;
    private final String vapidSubject;

    public WebPushSender(WebPushSubscriptionService subscriptions,
                         NotificationPreferenceService preferences,
                         VapidKeyProvider vapidKeys,
                         @Value("${app.webpush.enabled:true}") boolean enabled,
                         @Value("${app.webpush.vapid-subject:mailto:admin@paymentplatform.tn}") String vapidSubject) {
        this.subscriptions = subscriptions;
        this.preferences = preferences;
        this.vapidKeys = vapidKeys;
        this.enabled = enabled;
        this.vapidSubject = vapidSubject;
    }

    @Async
    public void sendToUser(UUID userId, String title, String body, String tag, String url) {
        if (!enabled || userId == null) {
            return;
        }
        try {
            if (!preferences.isPushEnabled(userId)) {
                log.debug("Web Push disabled by preference for user {}", userId);
                return;
            }
            for (WebPushSubscription subscription : subscriptions.list(userId)) {
                send(subscription, title, body, tag, url);
            }
        } catch (Exception e) {
            log.error("Web Push send failed for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Envoie aux appareils abonnes d'une organisation (les evenements
     * metier transportent des identifiants d'organisation, pas d'utilisateur).
     * Chaque abonnement reste filtre par la preference de son proprietaire.
     */
    @Async
    public void sendToOrganization(UUID organizationId, String title, String body, String tag, String url) {
        if (!enabled || organizationId == null) {
            return;
        }
        try {
            for (WebPushSubscription subscription : subscriptions.listByOrganization(organizationId)) {
                if (preferences.isPushEnabled(subscription.userId())) {
                    send(subscription, title, body, tag, url);
                }
            }
        } catch (Exception e) {
            log.error("Web Push send failed for organization {}: {}", organizationId, e.getMessage());
        }
    }

    private void send(WebPushSubscription subscription, String title, String body, String tag, String url) {
        try {
            PushService pushService = new PushService(vapidKeys.getKeyPair(), vapidSubject);
            String safeUrl = sanitizeUrl(url);
            Notification notification = new Notification(
                    subscription.endpoint(),
                    subscription.p256dh(),
                    subscription.auth(),
                    buildPayload(title, body, tag, safeUrl, subscription.organizationId()));

            HttpResponse response = pushService.send(notification);
            int status = response.getStatusLine().getStatusCode();
            if (status == 404 || status == 410) {
                subscriptions.removeByEndpoint(subscription.endpoint());
                log.info("Removed expired Web Push subscription (endpoint {}...)",
                        subscription.endpoint().substring(0, Math.min(40, subscription.endpoint().length())));
            } else if (status >= 400) {
                log.warn("Web Push endpoint rejected with status {}", status);
            } else {
                log.debug("Web Push sent to user {}", subscription.userId());
            }
        } catch (Exception e) {
            log.warn("Web Push delivery failed: {}", e.getMessage());
        }
    }

    /**
     * Construit le payload JSON attendu par ngsw-worker.js :
     * { notification: { title, body, icon, tag, data: { url, onActionClick } } }
     * Le data.url est injecté tel quel dans onActionClick (deep-link après clic).
     * navigateLastFocusedOrOpen : si la PWA est déjà ouverte, le clic navigue
     * dans cette fenêtre (pas seulement un focus) ; sinon il l'ouvre sur l'URL.
     */
    String buildPayload(String title, String body, String tag, String url,
                        UUID organizationId) throws Exception {
        Map<String, Object> data = new java.util.LinkedHashMap<>();
        data.put("url", url);
        data.put("organizationId", organizationId == null ? "" : organizationId.toString());
        data.put("onActionClick", Map.of("default", Map.of(
                "operation", "navigateLastFocusedOrOpen",
                "url", url)));

        Map<String, Object> notification = new java.util.LinkedHashMap<>();
        notification.put("title", title == null ? "Notification" : title);
        notification.put("body", body == null ? "" : body);
        notification.put("icon", "/assets/icons/icon-72x72.png");
        notification.put("badge", "/assets/icons/icon-72x72.png");
        notification.put("tag", tag == null ? "payment-notification" : tag);
        notification.put("data", data);

        return new com.fasterxml.jackson.databind.ObjectMapper()
                .writeValueAsString(Map.of("notification", notification));
    }

    /**
     * Rejecte les URLs dangereuses (javascript:, data:, protocole relatif //) —
     * fallback /dashboard. Accepte les deep-links relatifs et https (http en dev).
     */
    static String sanitizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "/dashboard";
        }
        String trimmed = url.trim();
        String lower = trimmed.toLowerCase();
        if (lower.startsWith("javascript:") || lower.startsWith("data:") || lower.startsWith("vbscript:")) {
            return "/dashboard";
        }
        if (trimmed.startsWith("//")) {
            return "/dashboard";
        }
        int colon = trimmed.indexOf(':');
        int slash = trimmed.indexOf('/');
        if (colon >= 0 && (slash < 0 || colon < slash)) {
            // Un schéma est présent : seul http(s) est accepté.
            if (!lower.startsWith("https://") && !lower.startsWith("http://")) {
                return "/dashboard";
            }
        }
        if (trimmed.length() > 2048) {
            return "/dashboard";
        }
        return trimmed;
    }
}
