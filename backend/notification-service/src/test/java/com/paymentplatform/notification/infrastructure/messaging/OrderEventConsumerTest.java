package com.paymentplatform.notification.infrastructure.messaging;

import com.paymentplatform.notification.domain.model.NotificationRepository;
import com.paymentplatform.shared.infrastructure.eventing.EventDeduplicator;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class OrderEventConsumerTest {

    @Autowired private OrderEventConsumer consumer;
    @Autowired private NotificationRepository notifications;
    @Autowired private EventDeduplicator deduplicator;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        notifications.deleteAll();
    }

    @Test
    void onOrderEvent_created_createsNotifications() throws Exception {
        String payload = """
                {"eventId":"evt-ord-1","eventType":"order.created","shopId":10,"supplierId":20,
                 "reference":"ORD-001","source":"SHOP"}
                """;
        Message message = createMessage(payload, "order.created");

        consumer.onOrderEvent(message);

        assertThat(notifications.count()).isEqualTo(2);
    }

    @Test
    void onOrderEvent_confirmed_createsNotification() throws Exception {
        String payload = """
                {"eventId":"evt-ord-2","eventType":"order.confirmed","shopId":10,"supplierId":20,
                 "reference":"ORD-002"}
                """;
        Message message = createMessage(payload, "order.confirmed");

        consumer.onOrderEvent(message);

        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    void onOrderEvent_preparing_createsNotification() throws Exception {
        String payload = """
                {"eventId":"evt-ord-3","eventType":"order.preparing","shopId":10,"supplierId":20,
                 "reference":"ORD-003"}
                """;
        Message message = createMessage(payload, "order.preparing");

        consumer.onOrderEvent(message);

        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    void onOrderEvent_readyForDelivery_createsNotification() throws Exception {
        String payload = """
                {"eventId":"evt-ord-4","eventType":"order.ready_for_delivery","shopId":10,"supplierId":20,
                 "reference":"ORD-004"}
                """;
        Message message = createMessage(payload, "order.ready_for_delivery");

        consumer.onOrderEvent(message);

        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    void onOrderEvent_delivered_createsNotification() throws Exception {
        String payload = """
                {"eventId":"evt-ord-5","eventType":"order.delivered","shopId":10,"supplierId":20,
                 "reference":"ORD-005"}
                """;
        Message message = createMessage(payload, "order.delivered");

        consumer.onOrderEvent(message);

        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    void onOrderEvent_accepted_createsNotification() throws Exception {
        String payload = """
                {"eventId":"evt-ord-6","eventType":"order.accepted","shopId":10,"supplierId":20,
                 "reference":"ORD-006"}
                """;
        Message message = createMessage(payload, "order.accepted");

        consumer.onOrderEvent(message);

        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    void onOrderEvent_cancelled_createsNotifications() throws Exception {
        String payload = """
                {"eventId":"evt-ord-7","eventType":"order.cancelled","shopId":10,"supplierId":20,
                 "reference":"ORD-007"}
                """;
        Message message = createMessage(payload, "order.cancelled");

        consumer.onOrderEvent(message);

        assertThat(notifications.count()).isEqualTo(2);
    }

    @Test
    void onOrderEvent_rejected_createsNotification() throws Exception {
        String payload = """
                {"eventId":"evt-ord-8","eventType":"order.rejected","shopId":10,"supplierId":20,
                 "reference":"ORD-008"}
                """;
        Message message = createMessage(payload, "order.rejected");

        consumer.onOrderEvent(message);

        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    void onOrderEvent_deliveryRejected_createsNotification() throws Exception {
        String payload = """
                {"eventId":"evt-ord-9","eventType":"order.delivery_rejected","shopId":10,"supplierId":20,
                 "reference":"ORD-009","reason":"Damaged goods"}
                """;
        Message message = createMessage(payload, "order.delivery_rejected");

        consumer.onOrderEvent(message);

        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    void onOrderEvent_deliveryConfirmed_createsNotifications() throws Exception {
        String payload = """
                {"eventId":"evt-ord-12","eventType":"order.delivery_confirmed","shopId":10,"supplierId":20,
                 "reference":"ORD-012","confirmedDate":"2026-09-15"}
                """;
        Message message = createMessage(payload, "order.delivery_confirmed");

        consumer.onOrderEvent(message);

        assertThat(notifications.count()).isEqualTo(2);
    }

    @Test
    void onOrderEvent_lowStockAlert_createsNotification() throws Exception {
        String payload = """
                {"eventId":"evt-ord-10","eventType":"order.low_stock_alert","supplierId":20,
                 "productName":"Widget","sku":"WDG-001","availableQty":3,"minQuantity":10}
                """;
        Message message = createMessage(payload, "order.low_stock_alert");

        consumer.onOrderEvent(message);

        assertThat(notifications.count()).isEqualTo(1);
    }

    @Test
    void onOrderEvent_duplicateEvent_skips() throws Exception {
        String payload = """
                {"eventId":"evt-ord-dup","eventType":"order.created","shopId":10,"supplierId":20,
                 "reference":"ORD-DUP","source":"SHOP"}
                """;
        Message message = createMessage(payload, "order.created");

        consumer.onOrderEvent(message);
        assertThat(notifications.count()).isEqualTo(2);
    }

    @Test
    void onOrderEvent_unknownRoutingKey_doesNothing() throws Exception {
        String payload = """
                {"eventId":"evt-ord-11","eventType":"order.unknown","shopId":10,"supplierId":20,
                 "reference":"ORD-011"}
                """;
        Message message = createMessage(payload, "order.unknown");

        consumer.onOrderEvent(message);

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
