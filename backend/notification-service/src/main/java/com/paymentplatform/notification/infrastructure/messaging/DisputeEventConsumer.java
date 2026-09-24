package com.paymentplatform.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.notification.domain.model.Notification;
import com.paymentplatform.notification.domain.model.NotificationRepository;
import com.paymentplatform.shared.infrastructure.eventing.EventDeduplicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class DisputeEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(DisputeEventConsumer.class);

    private final NotificationRepository notifications;
    private final EventDeduplicator deduplicator;
    private final ObjectMapper objectMapper;
    private final NotificationBroadcaster broadcaster;

    public DisputeEventConsumer(NotificationRepository notifications,
                                 EventDeduplicator deduplicator,
                                 ObjectMapper objectMapper,
                                 NotificationBroadcaster broadcaster) {
        this.notifications = notifications;
        this.deduplicator = deduplicator;
        this.objectMapper = objectMapper;
        this.broadcaster = broadcaster;
    }

    @RabbitListener(queues = "notification.disputes")
    public void onDisputeEvent(Message message) {
        try {
            JsonNode event = objectMapper.readTree(message.getBody());
            String eventId = event.get("eventId").asText();
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();

            if (!deduplicator.markProcessed(eventId)) {
                log.debug("Event {} already processed, skipping", eventId);
                return;
            }

            switch (routingKey) {
                case "dispute.created" -> handleDisputeCreated(event);
                case "dispute.message_added" -> handleDisputeMessageAdded(event);
                case "dispute.resolved" -> handleDisputeResolved(event);
                case "dispute.closed" -> handleDisputeClosed(event);
                default -> log.warn("Unknown dispute routing key: {}", routingKey);
            }
        } catch (Exception e) {
            log.error("Failed to process dispute event", e);
        }
    }

    private void handleDisputeCreated(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();
        String reason = event.get("reason").asText();

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "DISPUTE_CREATED",
                "Nouveau litige ouvert pour la commande " + reference + " : " + reason,
                "DISPUTE", event.get("disputeId").asText()
        )));

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "DISPUTE_CREATED",
                "Litige ouvert pour la commande " + reference,
                "DISPUTE", event.get("disputeId").asText()
        )));
    }

    private void handleDisputeMessageAdded(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String senderRole = event.get("senderRole").asText();
        UUID senderId = UUID.fromString(event.get("senderId").asText());

        UUID notifyShop = "SUPPLIER".equals(senderRole) ? shopId : null;
        UUID notifySupplier = "SHOP".equals(senderRole) ? supplierId : null;

        String senderLabel = "SUPPLIER".equals(senderRole) ? "Le fournisseur" : "La boutique";

        if (notifyShop != null) {
            broadcaster.broadcastNotification(notifications.save(new Notification(null, notifyShop,
                    "DISPUTE_MESSAGE",
                    senderLabel + " a ajouté un message au litige",
                    "DISPUTE", event.get("disputeId").asText()
            )));
        }
        if (notifySupplier != null) {
            broadcaster.broadcastNotification(notifications.save(new Notification(null, notifySupplier,
                    "DISPUTE_MESSAGE",
                    senderLabel + " a ajouté un message au litige",
                    "DISPUTE", event.get("disputeId").asText()
            )));
        }
    }

    private void handleDisputeResolved(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "DISPUTE_RESOLVED",
                "Le litige a été résolu",
                "DISPUTE", event.get("disputeId").asText()
        )));

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "DISPUTE_RESOLVED",
                "Le litige a été résolu",
                "DISPUTE", event.get("disputeId").asText()
        )));
    }

    private void handleDisputeClosed(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "DISPUTE_CLOSED",
                "Le litige a été fermé",
                "DISPUTE", event.get("disputeId").asText()
        )));

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "DISPUTE_CLOSED",
                "Le litige a été fermé",
                "DISPUTE", event.get("disputeId").asText()
        )));
    }
}
