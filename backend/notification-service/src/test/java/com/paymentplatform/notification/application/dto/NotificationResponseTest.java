package com.paymentplatform.notification.application.dto;

import com.paymentplatform.notification.domain.model.Notification;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
/**
 * Tests de NotificationResponseTest.
 * Perimetre : mapping/DTO NotificationResponse.
 * Moyens : JUnit pur (AssertJ).
 */

class NotificationResponseTest {

    @Test
    void from_createsResponse() {
        Notification n = new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), "PAYMENT_CREATED", "Test message", "PAYMENT", "REF-001");
        NotificationResponse response = NotificationResponse.from(n);

        assertThat(response.id()).isNull();
        assertThat(response.recipientUserId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(response.recipientOrganizationId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        assertThat(response.type()).isEqualTo("PAYMENT_CREATED");
        assertThat(response.message()).isEqualTo("Test message");
        assertThat(response.readStatus()).isEqualTo("UNREAD");
        assertThat(response.relatedEntityType()).isEqualTo("PAYMENT");
        assertThat(response.relatedEntityId()).isEqualTo("REF-001");
    }

    @Test
    void from_readNotification_includesReadAt() {
        Notification n = new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), null, "TYPE1", "msg", null, null);
        n.markAsRead();
        NotificationResponse response = NotificationResponse.from(n);

        assertThat(response.readStatus()).isEqualTo("READ");
        assertThat(response.readAt()).isNotNull();
    }

    @Test
    void from_nullOrgId() {
        Notification n = new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), null, "TYPE1", "msg", null, null);
        NotificationResponse response = NotificationResponse.from(n);
        assertThat(response.recipientOrganizationId()).isNull();
    }
}
