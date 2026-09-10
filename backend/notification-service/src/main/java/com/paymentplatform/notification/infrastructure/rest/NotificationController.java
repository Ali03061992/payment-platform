package com.paymentplatform.notification.infrastructure.rest;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.notification.application.dto.NotificationResponse;
import com.paymentplatform.notification.domain.model.Notification;
import com.paymentplatform.notification.domain.model.NotificationRepository;
import com.paymentplatform.notification.infrastructure.messaging.NotificationBroadcaster;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final NotificationRepository notifications;
    private final NotificationBroadcaster broadcaster;
    private final ObjectMapper objectMapper;

    @Value("${app.security.secret:}")
    private String jwtSecret;

    public NotificationController(NotificationRepository notifications,
                                   NotificationBroadcaster broadcaster,
                                   ObjectMapper objectMapper) {
        this.notifications = notifications;
        this.broadcaster = broadcaster;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal AuthenticatedUser user) {
        List<Notification> list;
        if (user.organizationId() != null) {
            list = notifications.findByRecipientOrganizationIdOrderByCreatedAtDesc(user.organizationId());
        } else {
            list = notifications.findByRecipientUserIdOrderByCreatedAtDesc(user.userId());
        }
        return ResponseEntity.ok(list.stream().map(NotificationResponse::from).toList());
    }

    @GetMapping("/unread-count")
    public ResponseEntity<Map<String, Long>> unreadCount(
            @AuthenticationPrincipal AuthenticatedUser user) {
        long count;
        if (user.organizationId() != null) {
            count = notifications.countByRecipientOrganizationIdAndReadStatus(user.organizationId(), "UNREAD");
        } else {
            count = notifications.countByRecipientUserIdAndReadStatus(user.userId(), "UNREAD");
        }
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/read-all")
    public ResponseEntity<Map<String, Integer>> markAllAsRead(
            @AuthenticationPrincipal AuthenticatedUser user) {
        int updated;
        if (user.organizationId() != null) {
            updated = notifications.markAllAsReadByOrgId(user.organizationId());
        } else {
            updated = notifications.markAllAsReadByUserId(user.userId());
        }
        return ResponseEntity.ok(Map.of("updated", updated));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable UUID id,
                                           @AuthenticationPrincipal AuthenticatedUser user) {
        Notification n = notifications.findById(id).orElseThrow(() ->
                new java.util.NoSuchElementException("Notification not found"));
        boolean belongsToOrg = user.organizationId() != null &&
                n.recipientOrganizationId() != null &&
                n.recipientOrganizationId().equals(user.organizationId());
        boolean belongsToUser = n.recipientUserId() != null &&
                n.recipientUserId().equals(user.userId());
        if (!belongsToOrg && !belongsToUser) {
            return ResponseEntity.status(403).build();
        }
        n.markAsRead();
        notifications.save(n);
        return ResponseEntity.noContent().build();
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@RequestParam String token) {
        SseEmitter emitter = new SseEmitter(0L);

        try {
            JsonNode payload = validateAndDecodeToken(token);
            if (payload == null) {
                emitter.completeWithError(new IllegalArgumentException("Token invalide"));
                return emitter;
            }

            UUID userId = payload.has("sub") ? UUID.fromString(payload.get("sub").asText()) : null;
            String orgIdStr = payload.has("organizationId") ? payload.get("organizationId").asText(null) : null;
            UUID orgId = orgIdStr != null ? UUID.fromString(orgIdStr) : null;

            if (orgId != null) {
                broadcaster.register("org:" + orgId, emitter);
            }
            if (userId != null) {
                broadcaster.register("user:" + userId, emitter);
            }

            log.debug("SSE client connected: userId={}, orgId={}", userId, orgId);
        } catch (Exception e) {
            log.warn("SSE connection failed: {}", e.getMessage());
            emitter.completeWithError(e);
        }

        return emitter;
    }

    private JsonNode validateAndDecodeToken(String token) {
        if (jwtSecret == null || jwtSecret.isEmpty()) return null;
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) return null;

            byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keyBytes, "HmacSHA256"));

            String expectedSignature = Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal((parts[0] + "." + parts[1]).getBytes(StandardCharsets.UTF_8)));

            if (!java.security.MessageDigest.isEqual(
                    parts[2].getBytes(StandardCharsets.UTF_8),
                    expectedSignature.getBytes(StandardCharsets.UTF_8))) {
                return null;
            }

            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
            JsonNode payloadNode = objectMapper.readTree(payloadJson);

            if (payloadNode.has("exp")) {
                long exp = payloadNode.get("exp").asLong();
                if (System.currentTimeMillis() / 1000 > exp) return null;
            }

            return payloadNode;
        } catch (Exception e) {
            return null;
        }
    }
}
