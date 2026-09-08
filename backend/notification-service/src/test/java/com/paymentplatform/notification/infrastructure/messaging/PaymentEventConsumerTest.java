package com.paymentplatform.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.notification.domain.model.NotificationRepository;
import com.paymentplatform.shared.infrastructure.eventing.EventDeduplicator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PaymentEventConsumerTest {

    @Autowired private PaymentEventConsumer consumer;
    @Autowired private NotificationRepository notifications;
    @Autowired private EventDeduplicator deduplicator;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        notifications.deleteAll();
    }

    @Test
    void onPaymentEvent_created_createsNotifications() throws Exception {
        String payload = """
                {"eventId":"evt-pay-1","eventType":"payment.created","shopId":10,"supplierId":20,
                 "reference":"PAY-001","createdBy":5,"amount":"100.00","currency":"TND"}
                """;
        Message message = createMessage(payload, "payment.created");

        consumer.onPaymentEvent(message);

        assertThat(notifications.count()).isEqualTo(2);
    }

    @Test
    void onPaymentEvent_confirmed_createsNotifications() throws Exception {
        String payload = """
                {"eventId":"evt-pay-2","eventType":"payment.confirmed","shopId":10,"supplierId":20,
                 "reference":"PAY-002","confirmedBy":20}
                """;
        Message message = createMessage(payload, "payment.confirmed");

        consumer.onPaymentEvent(message);

        assertThat(notifications.count()).isEqualTo(2);
    }

    @Test
    void onPaymentEvent_rejected_createsNotification() throws Exception {
        String payload = """
                {"eventId":"evt-pay-3","eventType":"payment.rejected","shopId":10,"supplierId":20,
                 "reference":"PAY-003","rejectionReason":"Insufficient funds"}
                """;
        Message message = createMessage(payload, "payment.rejected");

        consumer.onPaymentEvent(message);

        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    void onPaymentEvent_cancelled_createsNotifications() throws Exception {
        String payload = """
                {"eventId":"evt-pay-4","eventType":"payment.cancelled","shopId":10,"supplierId":20,
                 "reference":"PAY-004","cancelledBy":10}
                """;
        Message message = createMessage(payload, "payment.cancelled");

        consumer.onPaymentEvent(message);

        assertThat(notifications.count()).isEqualTo(2);
    }

    @Test
    void onPaymentEvent_duplicateEvent_skips() throws Exception {
        String payload = """
                {"eventId":"evt-pay-dup","eventType":"payment.created","shopId":10,"supplierId":20,
                 "reference":"PAY-DUP","createdBy":5,"amount":"50.00","currency":"TND"}
                """;
        Message message = createMessage(payload, "payment.created");

        consumer.onPaymentEvent(message);
        assertThat(notifications.count()).isEqualTo(2);
    }

    @Test
    void onPaymentEvent_unknownRoutingKey_doesNothing() throws Exception {
        String payload = """
                {"eventId":"evt-pay-5","eventType":"payment.unknown","shopId":10,"supplierId":20,
                 "reference":"PAY-005"}
                """;
        Message message = createMessage(payload, "payment.unknown");

        consumer.onPaymentEvent(message);

        assertThat(notifications.count()).isEqualTo(0);
    }

    private Message createMessage(String payload, String routingKey) {
        MessageProperties props = new MessageProperties();
        props.setReceivedRoutingKey(routingKey);
        props.setContentType("application/json");
        return MessageBuilder.withBody(payload.getBytes(StandardCharsets.UTF_8))
                .andProperties(props).build();
    }
}
