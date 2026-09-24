package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.OrderStatus;
import com.paymentplatform.organization.domain.repository.OrderEventRepository;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class CancelExpiredOrdersUseCase {

    private static final Logger log = LoggerFactory.getLogger(CancelExpiredOrdersUseCase.class);

    private static final int DRAFT_EXPIRY_DAYS = 7;
    private static final int CONFIRMED_EXPIRY_DAYS = 3;

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final ProductRepository products;
    private final OutboxEventStore outbox;

    public CancelExpiredOrdersUseCase(OrderRepository orders, OrderItemRepository orderItems,
                                      OrderEventRepository events, ProductRepository products,
                                      OutboxEventStore outbox) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.products = products;
        this.outbox = outbox;
    }

    @Transactional
    public void execute() {
        Instant now = Instant.now();

        cancelExpiredByStatus(OrderStatus.DRAFT.name(), DRAFT_EXPIRY_DAYS,
                "Annulée automatiquement : DRAFT depuis plus de " + DRAFT_EXPIRY_DAYS + " jours", now);

        cancelExpiredByStatus(OrderStatus.CONFIRMED.name(), CONFIRMED_EXPIRY_DAYS,
                "Annulée automatiquement : CONFIRMED depuis plus de " + CONFIRMED_EXPIRY_DAYS + " jours", now);
    }

    private void cancelExpiredByStatus(String status, int expiryDays, String reason, Instant now) {
        Instant cutoff = now.minus(expiryDays, ChronoUnit.DAYS);
        List<Order> expiredOrders = orders.findByStatusAndCreatedAtBefore(status, cutoff);
        log.info("Found {} expired orders in {} status (older than {} days)",
                expiredOrders.size(), status, expiryDays);

        for (Order order : expiredOrders) {
            cancelOrder(order, reason);
        }
    }

    private void cancelOrder(Order order, String reason) {
        try {
            List<OrderItem> items = orderItems.findByOrderId(order.getId());
            for (OrderItem item : items) {
                products.findByIdForUpdate(item.getProductId()).ifPresent(product -> {
                    product.setReservedQty(Math.max(0, product.getReservedQty() - item.getQuantity()));
                    products.save(product);
                });
            }

            order.cancel();
            orders.save(order);

            events.save(OrderEvent.create(order.getId(), "ORDER_AUTO_CANCELLED", null, reason));
            outbox.append(new OrderEvents.OrderAutoCancelledEvent(
                    UUID.randomUUID(), Instant.now(),
                    order.getId(), order.getReference(),
                    order.getShopId(), order.getSupplierId(), reason),
                    String.valueOf(order.getId()));

            log.info("Auto-cancelled order {} ({})", order.getReference(), reason);
        } catch (Exception e) {
            log.error("Failed to auto-cancel order {}: {}", order.getReference(), e.getMessage(), e);
        }
    }
}
