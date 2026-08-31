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

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final NotificationRepository notifications;
    private final EventDeduplicator deduplicator;
    private final ObjectMapper objectMapper;

    public PaymentEventConsumer(NotificationRepository notifications,
                                 EventDeduplicator deduplicator,
                                 ObjectMapper objectMapper) {
        this.notifications = notifications;
        this.deduplicator = deduplicator;
        this.objectMapper = objectMapper;
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
        long shopId = event.get("shopId").asLong();
        long supplierId = event.get("supplierId").asLong();
        String reference = event.get("reference").asText();
        long createdBy = event.get("createdBy").asLong();
        String amount = event.get("amount").asText();
        String currency = event.get("currency").asText();

        notifications.save(new Notification(
                0L, supplierId,
                "PAYMENT_CREATED",
                "Nouveau paiement " + reference + " de " + amount + " " + currency,
                "PAYMENT", reference
        ));

        notifications.save(new Notification(
                createdBy, shopId,
                "PAYMENT_CREATED",
                "Paiement " + reference + " de " + amount + " " + currency + " soumis",
                "PAYMENT", reference
        ));
    }

    private void handlePaymentConfirmed(JsonNode event) {
        long shopId = event.get("shopId").asLong();
        long supplierId = event.get("supplierId").asLong();
        String reference = event.get("reference").asText();
        long confirmedBy = event.get("confirmedBy").asLong();

        notifications.save(new Notification(
                0L, shopId,
                "PAYMENT_CONFIRMED",
                "Paiement " + reference + " confirm\u00e9 par le fournisseur",
                "PAYMENT", reference
        ));

        notifications.save(new Notification(
                confirmedBy, supplierId,
                "PAYMENT_CONFIRMED",
                "Paiement " + reference + " confirm\u00e9",
                "PAYMENT", reference
        ));
    }

    private void handlePaymentRejected(JsonNode event) {
        long shopId = event.get("shopId").asLong();
        String reference = event.get("reference").asText();
        String reason = event.has("rejectionReason") ? event.get("rejectionReason").asText() : "";

        notifications.save(new Notification(
                0L, shopId,
                "PAYMENT_REJECTED",
                "Paiement " + reference + " rejet\u00e9" + (reason.isEmpty() ? "" : " : " + reason),
                "PAYMENT", reference
        ));
    }

    private void handlePaymentCancelled(JsonNode event) {
        long shopId = event.get("shopId").asLong();
        long supplierId = event.get("supplierId").asLong();
        String reference = event.get("reference").asText();
        long cancelledBy = event.get("cancelledBy").asLong();

        notifications.save(new Notification(
                0L, supplierId,
                "PAYMENT_CANCELLED",
                "Paiement " + reference + " annul\u00e9 par la boutique",
                "PAYMENT", reference
        ));

        notifications.save(new Notification(
                cancelledBy, shopId,
                "PAYMENT_CANCELLED",
                "Paiement " + reference + " annul\u00e9",
                "PAYMENT", reference
        ));
    }
}
