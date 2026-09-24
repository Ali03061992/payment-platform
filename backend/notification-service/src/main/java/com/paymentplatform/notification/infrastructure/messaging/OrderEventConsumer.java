package com.paymentplatform.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.notification.domain.model.Notification;
import com.paymentplatform.notification.domain.model.NotificationRepository;
import com.paymentplatform.notification.infrastructure.email.EmailNotificationService;
import com.paymentplatform.notification.infrastructure.push.PushNotificationService;
import com.paymentplatform.shared.infrastructure.eventing.EventDeduplicator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final NotificationRepository notifications;
    private final EventDeduplicator deduplicator;
    private final ObjectMapper objectMapper;
    private final NotificationBroadcaster broadcaster;
    private final EmailNotificationService emailService;
    private final PushNotificationService pushService;

    public OrderEventConsumer(NotificationRepository notifications,
                               EventDeduplicator deduplicator,
                               ObjectMapper objectMapper,
                               NotificationBroadcaster broadcaster,
                               EmailNotificationService emailService,
                               PushNotificationService pushService) {
        this.notifications = notifications;
        this.deduplicator = deduplicator;
        this.objectMapper = objectMapper;
        this.broadcaster = broadcaster;
        this.emailService = emailService;
        this.pushService = pushService;
    }

    @RabbitListener(queues = "notification.orders")
    public void onOrderEvent(Message message) {
        try {
            JsonNode event = objectMapper.readTree(message.getBody());
            String eventId = event.get("eventId").asText();
            String routingKey = message.getMessageProperties().getReceivedRoutingKey();

            if (!deduplicator.markProcessed(eventId)) {
                log.debug("Event {} already processed, skipping", eventId);
                return;
            }

            switch (routingKey) {
                case "order.created" -> handleOrderCreated(event);
                case "order.confirmed" -> handleOrderConfirmed(event);
                case "order.preparing" -> handleOrderPreparing(event);
                case "order.ready_for_delivery" -> handleOrderReadyForDelivery(event);
                case "order.delivered" -> handleOrderDelivered(event);
                case "order.accepted" -> handleOrderAccepted(event);
                case "order.cancelled" -> handleOrderCancelled(event);
                case "order.auto_cancelled" -> handleOrderAutoCancelled(event);
                case "order.rejected" -> handleOrderRejected(event);
                case "order.delivery_confirmed" -> handleOrderDeliveryConfirmed(event);
                case "order.delivery_rejected" -> handleOrderDeliveryRejected(event);
                case "order.low_stock_alert" -> handleLowStockAlert(event);
                case "order.comment_created" -> handleOrderCommentCreated(event);
                default -> log.warn("Unknown order routing key: {}", routingKey);
            }
        } catch (Exception e) {
            log.error("Failed to process order event", e);
        }
    }

    private void handleOrderCreated(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();
        String source = event.get("source").asText();

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "ORDER_CREATED",
                "Nouvelle commande " + reference + (source.equals("SHOP") ? " de la boutique" : ""),
                "ORDER", reference
        )));

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "ORDER_CREATED",
                "Commande " + reference + " créée",
                "ORDER", reference
        )));

        pushService.sendToUser(supplierId, "Nouvelle commande",
                "Nouvelle commande " + reference + " reçue", "ORDER_CREATED", "/dashboard/supplier/orders");
    }

    private void handleOrderConfirmed(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        String reference = event.get("reference").asText();

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "ORDER_CONFIRMED",
                "Commande " + reference + " confirmée par le fournisseur",
                "ORDER", reference
        )));

        emailService.sendOrderConfirmation(null, "la boutique", reference, "");
    }

    private void handleOrderPreparing(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        String reference = event.get("reference").asText();

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "ORDER_PREPARING",
                "Commande " + reference + " en préparation",
                "ORDER", reference
        )));
    }

    private void handleOrderReadyForDelivery(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        String reference = event.get("reference").asText();

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "ORDER_READY",
                "Commande " + reference + " prête pour livraison",
                "ORDER", reference
        )));
    }

    private void handleOrderDelivered(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        String reference = event.get("reference").asText();

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "ORDER_DELIVERED",
                "Commande " + reference + " livrée — en attente d'acceptation",
                "ORDER", reference
        )));

        emailService.sendDeliveryNotification(null, "la boutique", reference, "");

        pushService.sendToUser(shopId, "Commande livrée",
                "La commande " + reference + " est prête à être acceptée", "ORDER_DELIVERED", "/dashboard/shop/orders");
    }

    private void handleOrderAccepted(JsonNode event) {
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "ORDER_ACCEPTED",
                "Commande " + reference + " acceptée par la boutique",
                "ORDER", reference
        )));

        pushService.sendToUser(supplierId, "Commande acceptée",
                "La commande " + reference + " a été acceptée par la boutique", "ORDER_ACCEPTED", "/dashboard/supplier/orders");
    }

    private void handleOrderCancelled(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "ORDER_CANCELLED",
                "Commande " + reference + " annulée",
                "ORDER", reference
        )));

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "ORDER_CANCELLED",
                "Commande " + reference + " annulée",
                "ORDER", reference
        )));
    }

    private void handleOrderAutoCancelled(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();
        String reason = event.has("reason") ? event.get("reason").asText() : "Annulée automatiquement";

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "ORDER_AUTO_CANCELLED",
                "Commande " + reference + " annulée automatiquement : " + reason,
                "ORDER", reference
        )));
    }

    private void handleOrderRejected(JsonNode event) {
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "ORDER_REJECTED",
                "Commande " + reference + " rejetée par la boutique",
                "ORDER", reference
        )));
    }

    private void handleOrderDeliveryConfirmed(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();
        String confirmedDate = event.has("confirmedDate") ? event.get("confirmedDate").asText() : "";

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "ORDER_DELIVERY_CONFIRMED",
                "Livraison de la commande " + reference + " confirmée pour le " + confirmedDate,
                "ORDER", reference
        )));

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "ORDER_DELIVERY_CONFIRMED",
                "Livraison de la commande " + reference + " confirmée pour le " + confirmedDate,
                "ORDER", reference
        )));
    }

    private void handleOrderDeliveryRejected(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        String reference = event.get("reference").asText();
        String reason = event.has("reason") ? event.get("reason").asText() : "";

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "ORDER_DELIVERY_REJECTED",
                "Livraison de la commande " + reference + " rejetée"
                        + (reason.isEmpty() ? "" : " : " + reason),
                "ORDER", reference
        )));
    }

    private void handleOrderCommentCreated(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        UUID authorId = UUID.fromString(event.get("authorId").asText());
        String authorName = event.get("authorName").asText();
        String reference = event.get("reference").asText();
        String content = event.get("content").asText();

        String message = "Nouveau commentaire de " + authorName + " sur la commande " + reference;

        UUID recipientOrgId = authorId.equals(shopId) ? supplierId : shopId;

        broadcaster.broadcastNotification(notifications.save(new Notification(null, recipientOrgId,
                "ORDER_COMMENT",
                message,
                "ORDER", reference
        )));
    }

    private void handleLowStockAlert(JsonNode event) {
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String productName = event.get("productName").asText();
        String sku = event.get("sku").asText();
        int availableQty = event.get("availableQty").asInt();
        int minQuantity = event.get("minQuantity").asInt();

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "LOW_STOCK_ALERT",
                "Stock bas pour " + productName + " (" + sku + ") — "
                        + availableQty + " disponible(s), minimum requis: " + minQuantity,
                "PRODUCT", sku
        )));
    }
}
