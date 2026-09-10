package com.paymentplatform.notification.infrastructure.messaging;

import java.util.UUID;

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

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final NotificationRepository notifications;
    private final EventDeduplicator deduplicator;
    private final ObjectMapper objectMapper;
    private final NotificationBroadcaster broadcaster;

    public PaymentEventConsumer(NotificationRepository notifications,
                                 EventDeduplicator deduplicator,
                                 ObjectMapper objectMapper,
                                 NotificationBroadcaster broadcaster) {
        this.notifications = notifications;
        this.deduplicator = deduplicator;
        this.objectMapper = objectMapper;
        this.broadcaster = broadcaster;
    }

    @RabbitListener(queues = "notification.payments")
    public void onPaymentEvent(Message message) {
        try {
            JsonNode event = objectMapper.readTree(message.getBody());
            String eventId = event.get("eventId").asText();
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();

            if (!deduplicator.markProcessed(eventId)) {
                log.debug("Event {} already processed, skipping", eventId);
                return;
            }

            switch (routingKey) {
                case "payment.created" -> handlePaymentCreated(event);
                case "payment.confirmed" -> handlePaymentConfirmed(event);
                case "payment.rejected" -> handlePaymentRejected(event);
                case "payment.cancelled" -> handlePaymentCancelled(event);
                default -> log.warn("Unknown payment routing key: {}", routingKey);
            }
        } catch (Exception e) {
            log.error("Failed to process payment event", e);
        }
    }

    private void handlePaymentCreated(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();
        UUID createdBy = UUID.fromString(event.get("createdBy").asText());
        String amount = event.get("amount").asText();
        String currency = event.get("currency").asText();

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "PAYMENT_CREATED",
                "Nouveau paiement " + reference + " de " + amount + " " + currency,
                "PAYMENT", reference
        )));

        broadcaster.broadcastNotification(notifications.save(new Notification(
                createdBy, shopId,
                "PAYMENT_CREATED",
                "Paiement " + reference + " de " + amount + " " + currency + " soumis",
                "PAYMENT", reference
        )));
    }

    private void handlePaymentConfirmed(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();
        UUID confirmedBy = UUID.fromString(event.get("confirmedBy").asText());

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "PAYMENT_CONFIRMED",
                "Paiement " + reference + " confirm\u00e9 par le fournisseur",
                "PAYMENT", reference
        )));

        broadcaster.broadcastNotification(notifications.save(new Notification(
                confirmedBy, supplierId,
                "PAYMENT_CONFIRMED",
                "Paiement " + reference + " confirm\u00e9",
                "PAYMENT", reference
        )));
    }

    private void handlePaymentRejected(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        String reference = event.get("reference").asText();
        String reason = event.has("rejectionReason") ? event.get("rejectionReason").asText() : "";

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "PAYMENT_REJECTED",
                "Paiement " + reference + " rejet\u00e9" + (reason.isEmpty() ? "" : " : " + reason),
                "PAYMENT", reference
        )));
    }

    private void handlePaymentCancelled(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();
        UUID cancelledBy = UUID.fromString(event.get("cancelledBy").asText());

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "PAYMENT_CANCELLED",
                "Paiement " + reference + " annul\u00e9 par la boutique",
                "PAYMENT", reference
        )));

        broadcaster.broadcastNotification(notifications.save(new Notification(
                cancelledBy, shopId,
                "PAYMENT_CANCELLED",
                "Paiement " + reference + " annul\u00e9",
                "PAYMENT", reference
        )));
    }
}
