package com.paymentplatform.organization.application.service;

import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.model.StockMovement;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.organization.domain.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StockOptimizationServiceTest {

    @Autowired private StockOptimizationService service;
    @Autowired private ProductRepository productRepository;
    @Autowired private StockMovementRepository movementRepository;

    @BeforeEach
    void setUp() {
        movementRepository.deleteAll();
        productRepository.deleteAll();
    }

    private Product createProduct(long supplierId, String sku, int qty, int minQty) {
        Product p = new Product();
        p.setSupplierId(supplierId);
        p.setName("Product-" + sku);
        p.setSku(sku);
        p.setUnitPrice(new BigDecimal("25.00"));
        p.setCurrency("TND");
        p.setQuantity(qty);
        p.setMinQuantity(minQty);
        p.setStatus("ACTIVE");
        p.setReservedQty(0);
        return productRepository.save(p);
    }

    private void createMovement(Product product, String type, int quantity) {
        StockMovement m = new StockMovement();
        m.setProductId(product.getId());
        m.setSupplierId(product.getSupplierId());
        m.setType(type);
        m.setQuantity(quantity);
        movementRepository.save(m);
    }

    @Test
    void optimize_withProducts_returnsResponse() {
        Product p = createProduct(10L, "SKU-001", 50, 10);
        createMovement(p, "OUT", 10);
        createMovement(p, "OUT", 15);

        var response = service.optimize(10L);

        assertThat(response).isNotNull();
        assertThat(response.products()).isNotNull();
    }

    @Test
    void optimize_noProducts_returnsEmptyResponse() {
        var response = service.optimize(99L);

        assertThat(response).isNotNull();
        assertThat(response.products()).isEmpty();
    }

    @Test
    void optimize_withNoMovements_returnsResponse() {
        createProduct(10L, "SKU-002", 100, 5);

        var response = service.optimize(10L);

        assertThat(response).isNotNull();
        assertThat(response.products()).hasSize(1);
    }

    @Test
    void setParameters_updatesConfig() {
        service.setParameters(14, 75.0, 0.30);

        createProduct(10L, "SKU-003", 20, 5);
        var response = service.optimize(10L);

        assertThat(response).isNotNull();
    }

    @Test
    void optimize_multipleProducts_returnsMultipleResults() {
        createProduct(10L, "SKU-A", 30, 5);
        createProduct(10L, "SKU-B", 80, 10);
        createProduct(10L, "SKU-C", 150, 20);

        var response = service.optimize(10L);

        assertThat(response.products()).hasSize(3);
    }

    @Test
    void optimize_productWithInMovements_derivesDemand() {
        Product p = createProduct(10L, "SKU-OUT", 100, 10);
        createMovement(p, "IN", 50);
        createMovement(p, "OUT", 20);
        createMovement(p, "OUT", 30);

        var response = service.optimize(10L);

        assertThat(response).isNotNull();
        assertThat(response.products()).hasSize(1);
    }
}
