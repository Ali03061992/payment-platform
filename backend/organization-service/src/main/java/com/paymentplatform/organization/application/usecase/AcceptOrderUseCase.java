package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.application.security.TenantAccess;
import com.paymentplatform.organization.domain.model.Order;
import com.paymentplatform.organization.domain.model.OrderEvent;
import com.paymentplatform.organization.domain.model.OrderItem;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.OrderEventRepository;
import com.paymentplatform.organization.domain.repository.OrderItemRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.organization.infrastructure.http.PaymentClient;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import com.paymentplatform.shared.domain.security.ActorContext;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class AcceptOrderUseCase {

    private static final Logger log = LoggerFactory.getLogger(AcceptOrderUseCase.class);

    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final OrderEventRepository events;
    private final ProductRepository products;
    private final OutboxEventStore outbox;
    private final IssueWithholdingCertificateUseCase withholding;
    private final PaymentClient paymentClient;

    public AcceptOrderUseCase(OrderRepository orders, OrderItemRepository orderItems,
                              OrderEventRepository events, ProductRepository products,
                              OutboxEventStore outbox,
                              IssueWithholdingCertificateUseCase withholding,
                              PaymentClient paymentClient) {
        this.orders = orders;
        this.orderItems = orderItems;
        this.events = events;
        this.products = products;
        this.outbox = outbox;
        this.withholding = withholding;
        this.paymentClient = paymentClient;
    }

    @Transactional
    public OrderResponse execute(UUID orderId, ActorContext actor) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        TenantAccess.requireShop(actor, order.getShopId());
        return accept(order, actor.userId());
    }

    @Deprecated
    @Transactional
    public OrderResponse execute(UUID orderId, UUID actorUserId) {
        Order order = orders.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Commande non trouvée : " + orderId));
        return accept(order, actorUserId);
    }

    private OrderResponse accept(Order order, UUID actorUserId) {
        UUID orderId = order.getId();
        if (!"DELIVERED".equals(order.getStatus())) {
            throw new ConflictException("La commande doit être en statut DELIVERED pour être acceptée");
        }

        order.accept();
        orders.save(order);

        try {
            paymentClient.createAutoPayment(order.getShopId(), order.getSupplierId(),
                    order.getCurrency(), actorUserId, order.getTotal(), orderId);
        } catch (Exception e) {
            log.error("Création du paiement auto impossible pour la commande {}", order.getReference(), e);
        }

        List<OrderItem> items = orderItems.findByOrderId(orderId);
        for (OrderItem item : items) {
            Product product = products.findByIdForUpdate(item.getProductId())
                    .orElseThrow(() -> new NotFoundException("Produit non trouvé : " + item.getProductId()));
            product.setQuantity(product.getQuantity() - item.getQuantity());
            product.setReservedQty(Math.max(0, product.getReservedQty() - item.getQuantity()));
            products.save(product);
        }

        events.save(OrderEvent.create(orderId, "ORDER_ACCEPTED", actorUserId, null));
        outbox.append(new OrderEvents.OrderAcceptedEvent(UUID.randomUUID(), Instant.now(),
                orderId, order.getReference(),
                order.getShopId(), order.getSupplierId(), actorUserId),
                String.valueOf(orderId));

        // Rattrapage : émet le certificat s'il n'existe pas encore (idempotent).
        try {
            withholding.issueForDeliveredOrder(order, null);
        } catch (Exception e) {
            log.error("Émission du certificat de retenue impossible pour la commande {}",
                    order.getReference(), e);
        }

        return OrderResponse.from(order, items);
    }
}
