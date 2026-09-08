package com.paymentplatform.notification;

import com.paymentplatform.notification.domain.model.Notification;
import com.paymentplatform.notification.domain.model.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

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
        Notification n = new Notification(10L, 20L, "ORDER_CREATED",
                "New order #123", "ORDER", "123");
        Notification saved = notifications.save(n);

        assertThat(saved.id()).isNotNull();
        assertThat(saved.readStatus()).isEqualTo("UNREAD");
        assertThat(saved.recipientUserId()).isEqualTo(10L);
        assertThat(saved.recipientOrganizationId()).isEqualTo(20L);
    }

    @Test
    void markAsRead_setsReadStatus() {
        Notification n = notifications.save(new Notification(10L, null, "PAYMENT_CONFIRMED",
                "Payment confirmed", "PAYMENT", "456"));
        n.markAsRead();
        notifications.save(n);

        var found = notifications.findById(n.id()).orElseThrow();
        assertThat(found.readStatus()).isEqualTo("READ");
        assertThat(found.readAt()).isNotNull();
    }

    @Test
    void findByRecipientUserId_returnsCorrectList() {
        notifications.save(new Notification(10L, null, "TYPE1", "msg1", null, null));
        notifications.save(new Notification(10L, null, "TYPE2", "msg2", null, null));
        notifications.save(new Notification(20L, null, "TYPE3", "msg3", null, null));

        var result = notifications.findByRecipientUserIdOrderByCreatedAtDesc(10L);
        assertThat(result).hasSize(2);
    }

    @Test
    void findByRecipientOrganizationId_returnsCorrectList() {
        notifications.save(new Notification(10L, 20L, "TYPE1", "msg1", null, null));
        notifications.save(new Notification(11L, 20L, "TYPE2", "msg2", null, null));
        notifications.save(new Notification(12L, 30L, "TYPE3", "msg3", null, null));

        var result = notifications.findByRecipientOrganizationIdOrderByCreatedAtDesc(20L);
        assertThat(result).hasSize(2);
    }

    @Test
    void countByRecipientUserIdAndReadStatus() {
        notifications.save(new Notification(10L, null, "T1", "m1", null, null));
        notifications.save(new Notification(10L, null, "T2", "m2", null, null));
        Notification read = notifications.save(new Notification(10L, null, "T3", "m3", null, null));
        read.markAsRead();
        notifications.save(read);

        long unread = notifications.countByRecipientUserIdAndReadStatus(10L, "UNREAD");
        long readCount = notifications.countByRecipientUserIdAndReadStatus(10L, "READ");
        assertThat(unread).isEqualTo(2);
        assertThat(readCount).isEqualTo(1);
    }

    @Test
    void markAllAsReadByUserId_updatesAll() {
        notifications.save(new Notification(10L, null, "T1", "m1", null, null));
        notifications.save(new Notification(10L, null, "T2", "m2", null, null));
        notifications.save(new Notification(20L, null, "T3", "m3", null, null));

        int updated = notifications.markAllAsReadByUserId(10L);
        assertThat(updated).isEqualTo(2);

        long unread = notifications.countByRecipientUserIdAndReadStatus(10L, "UNREAD");
        assertThat(unread).isEqualTo(0);
    }

    @Test
    void markAllAsReadByOrgId_updatesAll() {
        notifications.save(new Notification(10L, 20L, "T1", "m1", null, null));
        notifications.save(new Notification(11L, 20L, "T2", "m2", null, null));
        notifications.save(new Notification(12L, 30L, "T3", "m3", null, null));

        int updated = notifications.markAllAsReadByOrgId(20L);
        assertThat(updated).isEqualTo(2);

        long unread = notifications.countByRecipientOrganizationIdAndReadStatus(20L, "UNREAD");
        assertThat(unread).isEqualTo(0);
    }

    @Test
    void findById_returnsCorrectNotification() {
        Notification n = notifications.save(new Notification(10L, null, "FIND_ME", "find", null, null));
        var found = notifications.findById(n.id());
        assertThat(found).isPresent();
        assertThat(found.get().type()).isEqualTo("FIND_ME");
    }

    @Test
    void recipientUserIdOnly_noOrg() {
        Notification n = notifications.save(new Notification(10L, null, "USER_ONLY", "msg", null, null));
        assertThat(n.recipientOrganizationId()).isNull();
    }

    @Test
    void recipientOrgId_only() {
        Notification n = notifications.save(new Notification(1L, 20L, "ORG_ONLY", "msg", null, null));
        assertThat(n.recipientOrganizationId()).isEqualTo(20L);
    }
}
