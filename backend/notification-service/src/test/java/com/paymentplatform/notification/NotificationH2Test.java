package com.paymentplatform.notification;

import com.paymentplatform.notification.domain.model.Notification;
import com.paymentplatform.notification.domain.model.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class NotificationH2Test {

    @Autowired private NotificationRepository notifications;

    @BeforeEach
    void setUp() {
        notifications.deleteAll();
    }

    @Test
    void createNotification_savesCorrectly() {
        Notification n = new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), "ORDER_CREATED",
                "New order #123", "ORDER", "123");
        Notification saved = notifications.save(n);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.readStatus()).isEqualTo("UNREAD");
        assertThat(saved.recipientUserId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(saved.recipientOrganizationId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000020"));
    }

    @Test
    void markAsRead_setsReadStatus() {
        Notification n = notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), null, "PAYMENT_CONFIRMED",
                "Payment confirmed", "PAYMENT", "456"));
        n.markAsRead();
        notifications.save(n);

        var found = notifications.findById(n.id()).orElseThrow();
        assertThat(found.readStatus()).isEqualTo("READ");
        assertThat(found.readAt()).isNotNull();
    }

    @Test
    void findByRecipientUserId_returnsCorrectList() {
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), null, "TYPE1", "msg1", null, null));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), null, "TYPE2", "msg2", null, null));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000020"), null, "TYPE3", "msg3", null, null));

        var result = notifications.findByRecipientUserIdOrderByCreatedAtDesc(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(result).hasSize(2);
    }

    @Test
    void findByRecipientOrganizationId_returnsCorrectList() {
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), "TYPE1", "msg1", null, null));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000011"), UUID.fromString("00000000-0000-0000-0000-000000000020"), "TYPE2", "msg2", null, null));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000012"), UUID.fromString("00000000-0000-0000-0000-000000000030"), "TYPE3", "msg3", null, null));

        var result = notifications.findByRecipientOrganizationIdOrderByCreatedAtDesc(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        assertThat(result).hasSize(2);
    }

    @Test
    void countByRecipientUserIdAndReadStatus() {
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), null, "T1", "m1", null, null));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), null, "T2", "m2", null, null));
        Notification read = notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), null, "T3", "m3", null, null));
        read.markAsRead();
        notifications.save(read);

        long unread = notifications.countByRecipientUserIdAndReadStatus(UUID.fromString("00000000-0000-0000-0000-000000000010"), "UNREAD");
        long readCount = notifications.countByRecipientUserIdAndReadStatus(UUID.fromString("00000000-0000-0000-0000-000000000010"), "READ");
        assertThat(unread).isEqualTo(2);
        assertThat(readCount).isEqualTo(1);
    }

    @Test
    void markAllAsReadByUserId_updatesAll() {
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), null, "T1", "m1", null, null));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), null, "T2", "m2", null, null));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000020"), null, "T3", "m3", null, null));

        int updated = notifications.markAllAsReadByUserId(UUID.fromString("00000000-0000-0000-0000-000000000010"));
        assertThat(updated).isEqualTo(2);

        long unread = notifications.countByRecipientUserIdAndReadStatus(UUID.fromString("00000000-0000-0000-0000-000000000010"), "UNREAD");
        assertThat(unread).isEqualTo(0);
    }

    @Test
    void markAllAsReadByOrgId_updatesAll() {
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), UUID.fromString("00000000-0000-0000-0000-000000000020"), "T1", "m1", null, null));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000011"), UUID.fromString("00000000-0000-0000-0000-000000000020"), "T2", "m2", null, null));
        notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000012"), UUID.fromString("00000000-0000-0000-0000-000000000030"), "T3", "m3", null, null));

        int updated = notifications.markAllAsReadByOrgId(UUID.fromString("00000000-0000-0000-0000-000000000020"));
        assertThat(updated).isEqualTo(2);

        long unread = notifications.countByRecipientOrganizationIdAndReadStatus(UUID.fromString("00000000-0000-0000-0000-000000000020"), "UNREAD");
        assertThat(unread).isEqualTo(0);
    }

    @Test
    void findById_returnsCorrectNotification() {
        Notification n = notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), null, "FIND_ME", "find", null, null));
        var found = notifications.findById(n.id());
        assertThat(found).isPresent();
        assertThat(found.get().type()).isEqualTo("FIND_ME");
    }

    @Test
    void recipientUserIdOnly_noOrg() {
        Notification n = notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000010"), null, "USER_ONLY", "msg", null, null));
        assertThat(n.recipientOrganizationId()).isNull();
    }

    @Test
    void recipientOrgId_only() {
        Notification n = notifications.save(new Notification(UUID.fromString("00000000-0000-0000-0000-000000000001"), UUID.fromString("00000000-0000-0000-0000-000000000020"), "ORG_ONLY", "msg", null, null));
        assertThat(n.recipientOrganizationId()).isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000020"));
    }
}
