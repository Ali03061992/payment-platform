package com.paymentplatform.notification.infrastructure.rest;

import com.paymentplatform.notification.application.dto.NotificationResponse;
import com.paymentplatform.notification.domain.model.Notification;
import com.paymentplatform.notification.domain.model.NotificationRepository;
import com.paymentplatform.shared.infrastructure.security.AuthenticatedUser;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    private final NotificationRepository notifications;

    public NotificationController(NotificationRepository notifications) {
        this.notifications = notifications;
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
    public ResponseEntity<Void> markAsRead(@PathVariable Long id,
                                           @AuthenticationPrincipal AuthenticatedUser user) {
        Notification n = notifications.findById(id).orElseThrow(() ->
                new java.util.NoSuchElementException("Notification not found"));
        boolean belongsToOrg = user.organizationId() != null &&
                n.getRecipientOrganizationId() != null &&
                n.getRecipientOrganizationId().equals(user.organizationId());
        boolean belongsToUser = n.getRecipientUserId() != null &&
                n.getRecipientUserId().equals(user.userId());
        if (!belongsToOrg && !belongsToUser) {
            return ResponseEntity.status(403).build();
        }
        n.markAsRead();
        notifications.save(n);
        return ResponseEntity.noContent().build();
    }
}
