package com.paymentplatform.notification.infrastructure.messaging;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.paymentplatform.notification.domain.model.Notification;
import com.paymentplatform.notification.domain.model.NotificationRepository;
import com.paymentplatform.notification.infrastructure.email.EmailNotificationService;
import com.paymentplatform.notification.infrastructure.push.PushNotificationService;
import com.paymentplatform.notification.infrastructure.push.WebPushSender;
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
    private final WebPushSender webPushSender;

    public OrderEventConsumer(NotificationRepository notifications,
                               EventDeduplicator deduplicator,
                               ObjectMapper objectMapper,
                               NotificationBroadcaster broadcaster,
                               EmailNotificationService emailService,
                               PushNotificationService pushService,
                               WebPushSender webPushSender) {
        this.notifications = notifications;
        this.deduplicator = deduplicator;
        this.objectMapper = objectMapper;
        this.broadcaster = broadcaster;
        this.emailService = emailService;
        this.pushService = pushService;
        this.webPushSender = webPushSender;
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
                case "order.quote_requested" -> handleOrderQuoteRequested(event);
                case "order.quote_sent" -> handleOrderQuoteSent(event);
                case "order.quote_accepted" -> handleOrderQuoteAccepted(event);
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
        webPushSender.sendToOrganization(supplierId, "Nouvelle commande",
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

        webPushSender.sendToOrganization(shopId, "Commande confirmée",
                "La commande " + reference + " a été confirmée par le fournisseur",
                "ORDER_CONFIRMED", "/dashboard/shop/orders");
    }

    private void handleOrderPreparing(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        String reference = event.get("reference").asText();

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "ORDER_PREPARING",
                "Commande " + reference + " en préparation",
                "ORDER", reference
        )));

        webPushSender.sendToOrganization(shopId, "Commande en préparation",
                "La commande " + reference + " est en préparation",
                "ORDER_PREPARING", "/dashboard/shop/orders");
    }

    private void handleOrderReadyForDelivery(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        String reference = event.get("reference").asText();

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "ORDER_READY",
                "Commande " + reference + " prête pour livraison",
                "ORDER", reference
        )));

        webPushSender.sendToOrganization(shopId, "Commande prête",
                "La commande " + reference + " est prête pour livraison",
                "ORDER_READY", "/dashboard/shop/orders");
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
        webPushSender.sendToOrganization(shopId, "Commande livrée",
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
        webPushSender.sendToOrganization(supplierId, "Commande acceptée",
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

        webPushSender.sendToOrganization(supplierId, "Commande annulée",
                "La commande " + reference + " a été annulée",
                "ORDER_CANCELLED", "/dashboard/supplier/orders");
        webPushSender.sendToOrganization(shopId, "Commande annulée",
                "La commande " + reference + " a été annulée",
                "ORDER_CANCELLED", "/dashboard/shop/orders");
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

        webPushSender.sendToOrganization(shopId, "Commande annulée",
                "La commande " + reference + " a été annulée automatiquement : " + reason,
                "ORDER_AUTO_CANCELLED", "/dashboard/shop/orders");
    }

    private void handleOrderRejected(JsonNode event) {
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "ORDER_REJECTED",
                "Commande " + reference + " rejetée par la boutique",
                "ORDER", reference
        )));

        webPushSender.sendToOrganization(supplierId, "Commande rejetée",
                "La commande " + reference + " a été rejetée par la boutique",
                "ORDER_REJECTED", "/dashboard/supplier/orders");
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

        webPushSender.sendToOrganization(shopId, "Livraison confirmée",
                "Livraison de la commande " + reference + " confirmée pour le " + confirmedDate,
                "ORDER_DELIVERY_CONFIRMED", "/dashboard/shop/orders");
        webPushSender.sendToOrganization(supplierId, "Livraison confirmée",
                "Livraison de la commande " + reference + " confirmée pour le " + confirmedDate,
                "ORDER_DELIVERY_CONFIRMED", "/dashboard/supplier/orders");
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

        webPushSender.sendToOrganization(shopId, "Livraison rejetée",
                "La livraison de la commande " + reference + " a été rejetée"
                        + (reason.isEmpty() ? "" : " : " + reason),
                "ORDER_DELIVERY_REJECTED", "/dashboard/shop/orders");
    }

    private void handleOrderQuoteRequested(JsonNode event) {
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();
        String shopName = event.has("shopName") ? event.get("shopName").asText("") : "";

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "QUOTE_REQUESTED",
                "Nouvelle demande de devis " + reference
                        + (shopName.isEmpty() ? "" : " de " + shopName),
                "QUOTE", reference
        )));

        pushService.sendToUser(supplierId, "Demande de devis",
                "Nouvelle demande " + reference + " à chiffrer", "QUOTE_REQUESTED", "/dashboard/supplier/quotes");
        webPushSender.sendToOrganization(supplierId, "Demande de devis",
                "Nouvelle demande " + reference + " à chiffrer", "QUOTE_REQUESTED", "/dashboard/supplier/quotes");
    }

    private void handleOrderQuoteSent(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        String reference = event.get("reference").asText();
        int version = event.has("version") ? event.get("version").asInt(1) : 1;
        String total = event.has("total") ? event.get("total").asText("") : "";

        broadcaster.broadcastNotification(notifications.save(new Notification(null, shopId,
                "ORDER_QUOTE",
                "Devis n°" + version + " reçu pour la demande " + reference
                        + (total.isEmpty() ? "" : " — total " + total + " TND"),
                "QUOTE", reference
        )));

        pushService.sendToUser(shopId, "Devis reçu",
                "Devis n°" + version + " pour la demande " + reference, "ORDER_QUOTE", "/dashboard/shop/quotes");
        webPushSender.sendToOrganization(shopId, "Devis reçu",
                "Devis n°" + version + " pour la demande " + reference, "ORDER_QUOTE", "/dashboard/shop/quotes");
    }

    private void handleOrderQuoteAccepted(JsonNode event) {
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();
        String orderReference = event.has("orderReference") ? event.get("orderReference").asText("") : "";

        broadcaster.broadcastNotification(notifications.save(new Notification(null, supplierId,
                "QUOTE_ACCEPTED",
                "Devis " + reference + " accepté par la boutique"
                        + (orderReference.isEmpty() ? "" : " — commande " + orderReference + " créée"),
                "QUOTE", reference
        )));

        pushService.sendToUser(supplierId, "Devis accepté",
                "Devis " + reference + " accepté", "QUOTE_ACCEPTED", "/dashboard/supplier/quotes");
        webPushSender.sendToOrganization(supplierId, "Devis accepté",
                "Devis " + reference + " accepté", "QUOTE_ACCEPTED", "/dashboard/supplier/quotes");
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

        webPushSender.sendToOrganization(recipientOrgId, "Nouveau commentaire",
                message, "ORDER_COMMENT", "/dashboard/orders");
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

        webPushSender.sendToOrganization(supplierId, "Stock bas",
                productName + " (" + sku + ") : " + availableQty
                        + " disponible(s), minimum requis " + minQuantity,
                "LOW_STOCK_ALERT", "/dashboard/supplier/products");
    }
}
