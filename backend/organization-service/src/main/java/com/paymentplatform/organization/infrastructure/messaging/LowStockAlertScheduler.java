package com.paymentplatform.organization.infrastructure.messaging;

import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.shared.domain.event.OrderEvents;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class LowStockAlertScheduler {

    private static final Logger log = LoggerFactory.getLogger(LowStockAlertScheduler.class);

    private final ProductRepository productRepository;
    private final OutboxEventStore outboxEventStore;

    public LowStockAlertScheduler(ProductRepository productRepository, OutboxEventStore outboxEventStore) {
        this.productRepository = productRepository;
        this.outboxEventStore = outboxEventStore;
    }

    @Scheduled(fixedRate = 6 * 60 * 60 * 1000)
    public void checkLowStock() {
        List<Product> lowStockProducts = productRepository.findLowStockProducts();
        log.info("Found {} low-stock products", lowStockProducts.size());

        for (Product product : lowStockProducts) {
            int availableQty = product.getQuantity() - product.getReservedQty();
            OrderEvents.LowStockAlertEvent event = new OrderEvents.LowStockAlertEvent(
                    UUID.randomUUID(),
                    Instant.now(),
                    product.getId(),
                    product.getName(),
                    product.getSku(),
                    availableQty,
                    product.getMinQuantity(),
                    product.getSupplierId()
            );
            outboxEventStore.append(event, String.valueOf(product.getId()));
            log.info("Published low stock alert for product {} (available: {}, min: {})",
                    product.getSku(), availableQty, product.getMinQuantity());
        }
    }
}
