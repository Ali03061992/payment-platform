package com.paymentplatform.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.notification.domain.model.Notification;
import com.paymentplatform.notification.domain.model.NotificationRepository;
import com.paymentplatform.notification.infrastructure.email.EmailNotificationService;
import com.paymentplatform.shared.infrastructure.eventing.EventDeduplicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class IdentityEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(IdentityEventConsumer.class);

    private final NotificationRepository notifications;
    private final EventDeduplicator deduplicator;
    private final ObjectMapper objectMapper;
    private final NotificationBroadcaster broadcaster;
    private final EmailNotificationService emailService;

    public IdentityEventConsumer(NotificationRepository notifications,
                                  EventDeduplicator deduplicator,
                                  ObjectMapper objectMapper,
                                  NotificationBroadcaster broadcaster,
                                  EmailNotificationService emailService) {
        this.notifications = notifications;
        this.deduplicator = deduplicator;
        this.objectMapper = objectMapper;
        this.broadcaster = broadcaster;
        this.emailService = emailService;
    }

    @RabbitListener(queues = "notification.users")
    public void onIdentityEvent(Message message) {
        try {
            JsonNode event = objectMapper.readTree(message.getBody());
            String eventId = event.get("eventId").asText();
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();

            if (!deduplicator.markProcessed(eventId)) {
                log.debug("Event {} already processed, skipping", eventId);
                return;
            }

            switch (routingKey) {
                case "identity.user.created" -> handleUserCreated(event);
                default -> log.debug("Unhandled identity routing key: {}", routingKey);
            }
        } catch (Exception e) {
            log.error("Failed to process identity event", e);
        }
    }

    private void handleUserCreated(JsonNode event) {
        JsonNode rolesNode = event.get("roles");
        if (rolesNode != null && rolesNode.isArray()) {
            for (JsonNode role : rolesNode) {
                if ("SUPPLIER_AGENT".equals(role.asText())) {
                    UUID organizationId = UUID.fromString(event.get("organizationId").asText());
                    UUID userId = UUID.fromString(event.get("userId").asText());

                    broadcaster.broadcastNotification(notifications.save(new Notification(null,
                            organizationId,
                            "AGENT_INVITED",
                            "Nouvel agent livreur invité",
                            "USER", userId.toString()
                    )));

                    emailService.sendAgentInvitation(null, "l'agent", "Payment Platform");
                    return;
                }
            }
        }
    }
}
