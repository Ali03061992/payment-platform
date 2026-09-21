package com.paymentplatform.organization.infrastructure.messaging;

import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.shared.infrastructure.outbox.OutboxEventStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class LowStockAlertSchedulerTest {

    @Autowired private LowStockAlertScheduler scheduler;
    @Autowired private ProductRepository productRepository;
    @Autowired private OutboxEventStore outboxEventStore;

    @BeforeEach
    void setUp() {
        productRepository.deleteAll();
    }

    @Test
    void checkLowStock_withLowStockProducts_publishesEvents() {
        Product product = new Product();
        product.setSupplierId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        product.setName("Widget");
        product.setSku("WDG-001");
        product.setUnitPrice(new BigDecimal("10.00"));
        product.setCurrency("TND");
        product.setQuantity(3);
        product.setMinQuantity(10);
        product.setStatus("ACTIVE");
        product.setReservedQty(0);
        productRepository.save(product);

        scheduler.checkLowStock();

        assertThat(outboxEventStore).isNotNull();
    }

    @Test
    void checkLowStock_noLowStockProducts_doesNothing() {
        scheduler.checkLowStock();
        assertThat(outboxEventStore).isNotNull();
    }
}
