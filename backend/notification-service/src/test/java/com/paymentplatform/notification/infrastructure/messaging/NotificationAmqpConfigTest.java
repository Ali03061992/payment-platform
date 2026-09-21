package com.paymentplatform.notification.infrastructure.messaging;

import com.paymentplatform.shared.infrastructure.outbox.AmqpTopology;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class NotificationAmqpConfigTest {

    @Autowired private TopicExchange paymentExchange;
    @Autowired private TopicExchange organizationExchange;
    @Autowired private TopicExchange identityExchange;
    @Autowired private Queue notificationPaymentsQueue;
    @Autowired private Queue notificationOrdersQueue;
    @Autowired private Queue notificationOrganizationsQueue;
    @Autowired private Queue notificationUsersQueue;

    @Test
    void exchanges_areCorrect() {
        assertThat(paymentExchange.getName()).isEqualTo(AmqpTopology.EXCHANGE_PAYMENT);
        assertThat(organizationExchange.getName()).isEqualTo(AmqpTopology.EXCHANGE_ORGANIZATION);
        assertThat(identityExchange.getName()).isEqualTo(AmqpTopology.EXCHANGE_IDENTITY);
    }

    @Test
    void paymentQueue_isDurable() {
        assertThat(notificationPaymentsQueue.getName()).isEqualTo(AmqpTopology.QUEUE_NOTIFICATION_PAYMENTS);
        assertThat(notificationPaymentsQueue.isDurable()).isTrue();
    }

    @Test
    void ordersQueue_isDurable() {
        assertThat(notificationOrdersQueue.getName()).isEqualTo(AmqpTopology.QUEUE_NOTIFICATION_ORDERS);
        assertThat(notificationOrdersQueue.isDurable()).isTrue();
    }

    @Test
    void organizationsQueue_isDurable() {
        assertThat(notificationOrganizationsQueue.getName()).isEqualTo(AmqpTopology.QUEUE_NOTIFICATION_ORGANIZATIONS);
        assertThat(notificationOrganizationsQueue.isDurable()).isTrue();
    }

    @Test
    void usersQueue_isDurable() {
        assertThat(notificationUsersQueue.getName()).isEqualTo(AmqpTopology.QUEUE_NOTIFICATION_USERS);
        assertThat(notificationUsersQueue.isDurable()).isTrue();
    }
}
