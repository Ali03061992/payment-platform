package com.paymentplatform.notification.application.dto;

import com.paymentplatform.notification.domain.model.Notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        Long recipientUserId,
        Long recipientOrganizationId,
        String type,
        String message,
        String readStatus,
        Instant createdAt,
        Instant readAt,
        String relatedEntityType,
        String relatedEntityId
) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.id(), n.recipientUserId(), n.recipientOrganizationId(),
                n.type(), n.message(), n.readStatus(),
                n.createdAt(), n.readAt(),
                n.relatedEntityType(), n.relatedEntityId()
        );
    }
}
