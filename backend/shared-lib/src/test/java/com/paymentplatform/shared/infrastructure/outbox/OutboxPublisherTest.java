package com.paymentplatform.shared.infrastructure.outbox;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class OutboxPublisherTest {

    @Test
    void publish_sendsMessage() {
        org.springframework.amqp.rabbit.core.RabbitTemplate rabbitTemplate = mock(org.springframework.amqp.rabbit.core.RabbitTemplate.class);
        OutboxPublisher publisher = new OutboxPublisher(rabbitTemplate);

        publisher.publish("payment.events", "payment.created", "evt-123", "{\"key\":\"value\"}");

        verify(rabbitTemplate).convertAndSend(
                eq("payment.events"),
                eq("payment.created"),
                any(org.springframework.amqp.core.Message.class));
    }
}
