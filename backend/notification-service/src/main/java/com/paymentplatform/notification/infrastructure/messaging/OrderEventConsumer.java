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
public class OrderEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventConsumer.class);

    private final NotificationRepository notifications;
    private final EventDeduplicator deduplicator;
    private final ObjectMapper objectMapper;

    public OrderEventConsumer(NotificationRepository notifications,
                               EventDeduplicator deduplicator,
                               ObjectMapper objectMapper) {
        this.notifications = notifications;
        this.deduplicator = deduplicator;
        this.objectMapper = objectMapper;
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
                case "order.rejected" -> handleOrderRejected(event);
                case "order.delivery_rejected" -> handleOrderDeliveryRejected(event);
                case "order.low_stock_alert" -> handleLowStockAlert(event);
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

        notifications.save(new Notification(null, supplierId,
                "ORDER_CREATED",
                "Nouvelle commande " + reference + (source.equals("SHOP") ? " de la boutique" : ""),
                "ORDER", reference
        ));

        notifications.save(new Notification(null, shopId,
                "ORDER_CREATED",
                "Commande " + reference + " créée",
                "ORDER", reference
        ));
    }

    private void handleOrderConfirmed(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        String reference = event.get("reference").asText();

        notifications.save(new Notification(null, shopId,
                "ORDER_CONFIRMED",
                "Commande " + reference + " confirmée par le fournisseur",
                "ORDER", reference
        ));
    }

    private void handleOrderPreparing(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        String reference = event.get("reference").asText();

        notifications.save(new Notification(null, shopId,
                "ORDER_PREPARING",
                "Commande " + reference + " en préparation",
                "ORDER", reference
        ));
    }

    private void handleOrderReadyForDelivery(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        String reference = event.get("reference").asText();

        notifications.save(new Notification(null, shopId,
                "ORDER_READY",
                "Commande " + reference + " prête pour livraison",
                "ORDER", reference
        ));
    }

    private void handleOrderDelivered(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        String reference = event.get("reference").asText();

        notifications.save(new Notification(null, shopId,
                "ORDER_DELIVERED",
                "Commande " + reference + " livrée — en attente d'acceptation",
                "ORDER", reference
        ));
    }

    private void handleOrderAccepted(JsonNode event) {
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();

        notifications.save(new Notification(null, supplierId,
                "ORDER_ACCEPTED",
                "Commande " + reference + " acceptée par la boutique",
                "ORDER", reference
        ));
    }

    private void handleOrderCancelled(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();

        notifications.save(new Notification(null, supplierId,
                "ORDER_CANCELLED",
                "Commande " + reference + " annulée",
                "ORDER", reference
        ));

        notifications.save(new Notification(null, shopId,
                "ORDER_CANCELLED",
                "Commande " + reference + " annulée",
                "ORDER", reference
        ));
    }

    private void handleOrderRejected(JsonNode event) {
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String reference = event.get("reference").asText();

        notifications.save(new Notification(null, supplierId,
                "ORDER_REJECTED",
                "Commande " + reference + " rejetée par la boutique",
                "ORDER", reference
        ));
    }

    private void handleOrderDeliveryRejected(JsonNode event) {
        UUID shopId = UUID.fromString(event.get("shopId").asText());
        String reference = event.get("reference").asText();
        String reason = event.has("reason") ? event.get("reason").asText() : "";

        notifications.save(new Notification(null, shopId,
                "ORDER_DELIVERY_REJECTED",
                "Livraison de la commande " + reference + " rejetée"
                        + (reason.isEmpty() ? "" : " : " + reason),
                "ORDER", reference
        ));
    }

    private void handleLowStockAlert(JsonNode event) {
        UUID supplierId = UUID.fromString(event.get("supplierId").asText());
        String productName = event.get("productName").asText();
        String sku = event.get("sku").asText();
        int availableQty = event.get("availableQty").asInt();
        int minQuantity = event.get("minQuantity").asInt();

        notifications.save(new Notification(null, supplierId,
                "LOW_STOCK_ALERT",
                "Stock bas pour " + productName + " (" + sku + ") — "
                        + availableQty + " disponible(s), minimum requis: " + minQuantity,
                "PRODUCT", sku
        ));
    }
}
