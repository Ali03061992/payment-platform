package com.paymentplatform.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.notification.domain.model.Notification;
import com.paymentplatform.notification.domain.model.NotificationRepository;
import com.paymentplatform.notification.infrastructure.http.IdentityAdminClient;
import com.paymentplatform.notification.infrastructure.push.WebPushSender;
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
    private static final String DISPUTE_LINK = "/dashboard/reclamations/";

    private final NotificationRepository notifications;
    private final EventDeduplicator deduplicator;
    private final ObjectMapper objectMapper;
    private final NotificationBroadcaster broadcaster;
    private final IdentityAdminClient adminClient;
    private final WebPushSender webPushSender;

    public DisputeEventConsumer(NotificationRepository notifications,
                                 EventDeduplicator deduplicator,
                                 ObjectMapper objectMapper,
                                 NotificationBroadcaster broadcaster,
                                 IdentityAdminClient adminClient,
                                 WebPushSender webPushSender) {
        this.notifications = notifications;
        this.deduplicator = deduplicator;
        this.objectMapper = objectMapper;
        this.broadcaster = broadcaster;
        this.adminClient = adminClient;
        this.webPushSender = webPushSender;
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
                "DISPUTE_CREATED_SUPPLIER",
                params("reference", reference, "reason", reason),
                "DISPUTE", event.get("disputeId").asText()
        )));

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "DISPUTE_CREATED",
                "Litige ouvert pour la commande " + reference,
                "DISPUTE_CREATED_SHOP",
                params("reference", reference),
                "DISPUTE", event.get("disputeId").asText()
        )));

        webPushSender.sendToOrganization(supplierId, "Nouveau litige",
                "Litige ouvert pour la commande " + reference, "DISPUTE_CREATED",
                DISPUTE_LINK + event.get("disputeId").asText());
        webPushSender.sendToOrganization(shopId, "Nouveau litige",
                "Litige ouvert pour la commande " + reference, "DISPUTE_CREATED",
                DISPUTE_LINK + event.get("disputeId").asText());

        notifyAdmins("DISPUTE_CREATED",
                "DISPUTE_CREATED_ADMIN",
                params("reference", reference, "reason", reason),
                "Nouvelle réclamation (commande " + reference + ") : " + reason,
                event.get("disputeId").asText());
    }

    private void handleDisputeMessageAdded(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String senderRole = event.get("senderRole").asText();
        UUID senderId = UUID.fromString(event.get("senderId").asText());

        UUID notifyShop = "SUPPLIER".equals(senderRole) ? shopId : null;
        UUID notifySupplier = "SHOP".equals(senderRole) ? supplierId : null;

        String senderLabel = "SUPPLIER".equals(senderRole) ? "Le fournisseur" : "La boutique";
        String messageKey = "SUPPLIER".equals(senderRole)
                ? "DISPUTE_MESSAGE_FROM_SUPPLIER" : "DISPUTE_MESSAGE_FROM_SHOP";
        String adminKey = "SUPPLIER".equals(senderRole)
                ? "DISPUTE_MESSAGE_ADMIN_FROM_SUPPLIER" : "DISPUTE_MESSAGE_ADMIN_FROM_SHOP";

        if (notifyShop != null) {
            broadcaster.broadcastNotification(notifications.save(new Notification(null, notifyShop,
                    "DISPUTE_MESSAGE",
                    senderLabel + " a ajouté un message au litige",
                    messageKey, params(),
                    "DISPUTE", event.get("disputeId").asText()
            )));
        }
        if (notifySupplier != null) {
            broadcaster.broadcastNotification(notifications.save(new Notification(null, notifySupplier,
                    "DISPUTE_MESSAGE",
                    senderLabel + " a ajouté un message au litige",
                    messageKey, params(),
                    "DISPUTE", event.get("disputeId").asText()
            )));
        }

        if (notifyShop != null) {
            webPushSender.sendToOrganization(notifyShop, "Nouveau message",
                    senderLabel + " a ajouté un message au litige",
                    "DISPUTE_MESSAGE", DISPUTE_LINK + event.get("disputeId").asText());
        }
        if (notifySupplier != null) {
            webPushSender.sendToOrganization(notifySupplier, "Nouveau message",
                    senderLabel + " a ajouté un message au litige",
                    "DISPUTE_MESSAGE", DISPUTE_LINK + event.get("disputeId").asText());
        }

        notifyAdmins("DISPUTE_MESSAGE",
                adminKey,
                params(),
                senderLabel + " a ajouté un message à une réclamation",
                event.get("disputeId").asText());
    }

    private void handleDisputeResolved(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "DISPUTE_RESOLVED",
                "Le litige a été résolu",
                "DISPUTE_RESOLVED", params(),
                "DISPUTE", event.get("disputeId").asText()
        )));

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "DISPUTE_RESOLVED",
                "Le litige a été résolu",
                "DISPUTE_RESOLVED", params(),
                "DISPUTE", event.get("disputeId").asText()
        )));

        webPushSender.sendToOrganization(shopId, "Litige résolu",
                "Le litige a été résolu", "DISPUTE_RESOLVED",
                DISPUTE_LINK + event.get("disputeId").asText());
        webPushSender.sendToOrganization(supplierId, "Litige résolu",
                "Le litige a été résolu", "DISPUTE_RESOLVED",
                DISPUTE_LINK + event.get("disputeId").asText());
    }

    /**
     * Notifie tous les SYSTEM_ADMIN (adressage direct par userId : ils n'ont
     * pas d'organisation). Échec silencieux : ne bloque jamais le flux boutique.
     */
    private void notifyAdmins(String type, String messageKey, String messageParams,
                              String message, String disputeId) {
        try {
            for (UUID adminId : adminClient.systemAdminIds()) {
                broadcaster.broadcastNotification(notifications.save(
                        new Notification(adminId, null, type, message, messageKey, messageParams,
                                "DISPUTE", disputeId)));
                webPushSender.sendToUser(adminId, "Réclamation",
                        message, type, DISPUTE_LINK + disputeId);
            }
        } catch (Exception e) {
            log.error("Notification des admins impossible", e);
        }
    }

    private String params(String... keyValues) {
        var node = objectMapper.createObjectNode();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            node.put(keyValues[i], keyValues[i + 1]);
        }
        return node.toString();
    }

    private void handleDisputeClosed(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "DISPUTE_CLOSED",
                "Le litige a été fermé",
                "DISPUTE_CLOSED", params(),
                "DISPUTE", event.get("disputeId").asText()
        )));

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "DISPUTE_CLOSED",
                "Le litige a été fermé",
                "DISPUTE_CLOSED", params(),
                "DISPUTE", event.get("disputeId").asText()
        )));

        webPushSender.sendToOrganization(shopId, "Litige fermé",
                "Le litige a été fermé", "DISPUTE_CLOSED",
                DISPUTE_LINK + event.get("disputeId").asText());
        webPushSender.sendToOrganization(supplierId, "Litige fermé",
                "Le litige a été fermé", "DISPUTE_CLOSED",
                DISPUTE_LINK + event.get("disputeId").asText());
    }
}
