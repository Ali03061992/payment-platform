package com.paymentplatform.shared.infrastructure.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/** Publication d'un événement d'outbox sur RabbitMQ (idempotente côté consommateur). */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);

    private final RabbitTemplate rabbitTemplate;

    public OutboxPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publish(String exchange, String routingKey, String eventId, String jsonPayload) {
        Message message = MessageBuilder.withBody(jsonPayload.getBytes(java.nio.charset.StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setMessageId(eventId)
                .build();
        rabbitTemplate.convertAndSend(exchange, routingKey, message);
        log.info("Événement publié sur {}/{} (id={})", exchange, routingKey, eventId);
    }
}