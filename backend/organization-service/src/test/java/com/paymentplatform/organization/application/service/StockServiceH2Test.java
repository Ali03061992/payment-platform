package com.paymentplatform.organization.application.service;

import com.paymentplatform.organization.application.dto.ProductCreateRequest;
import com.paymentplatform.organization.application.dto.ProductUpdateRequest;
import com.paymentplatform.organization.application.dto.StockMovementRequest;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.shared.domain.exception.ConflictException;
import com.paymentplatform.shared.domain.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
/**
 * Tests de StockServiceH2Test.
 * Perimetre : cas d'usage/service StockService sur base H2.
 * Moyens : contexte SpringBootTest, profil "test" (H2).
 */

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class StockServiceH2Test {

    @Autowired private StockService stockService;
    @Autowired private ProductRepository products;

    private UUID supplierId;

    @BeforeEach
    void setUp() {
        supplierId = UUID.fromString("00000000-0000-0000-0000-000000000001");
    }

    @Test
    void createProduct_validRequest_createsProduct() {
        var request = new ProductCreateRequest("Widget", "WDG-001", "A widget", null,
                new BigDecimal("15.50"), "TND", 100, 10);
        var response = stockService.createProduct(supplierId, request);

        assertThat(response.id()).isNotNull();
        assertThat(response.name()).isEqualTo("Widget");
        assertThat(response.sku()).isEqualTo("WDG-001");
        assertThat(response.quantity()).isEqualTo(100);
    }

    @Test
    void createProduct_duplicateSku_throwsConflict() {
        var request1 = new ProductCreateRequest("Widget", "DUP-001", null, null,
                new BigDecimal("10.00"), "TND", 50, 5);
        stockService.createProduct(supplierId, request1);

        var request2 = new ProductCreateRequest("Widget2", "DUP-001", null, null,
                new BigDecimal("20.00"), "TND", 30, 3);
        assertThatThrownBy(() -> stockService.createProduct(supplierId, request2))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("SKU");
    }

    @Test
    void listProducts_returnsAll() {
        stockService.createProduct(supplierId, new ProductCreateRequest("P1", "P1-001", null, null,
                new BigDecimal("10.00"), "TND", 10, 1));
        stockService.createProduct(supplierId, new ProductCreateRequest("P2", "P2-001", null, null,
                new BigDecimal("20.00"), "TND", 20, 2));
        var result = stockService.listProducts(supplierId, null);
        assertThat(result).hasSize(2);
    }

    @Test
    void listProducts_filterByStatus() {
        stockService.createProduct(supplierId, new ProductCreateRequest("Active", "ACT-001", null, null,
                new BigDecimal("10.00"), "TND", 10, 1));
        var inactiveProduct = stockService.createProduct(supplierId, new ProductCreateRequest("Inactive", "INA-001", null, null,
                new BigDecimal("10.00"), "TND", 10, 1));
        stockService.deleteProduct(supplierId, inactiveProduct.id());
        var active = stockService.listProducts(supplierId, "ACTIVE");
        assertThat(active).hasSize(1);
    }

    @Test
    void getProduct_valid_returnsProduct() {
        var created = stockService.createProduct(supplierId, new ProductCreateRequest("Get", "GET-001", null, null,
                new BigDecimal("10.00"), "TND", 10, 1));
        var found = stockService.getProduct(supplierId, created.id());
        assertThat(found.name()).isEqualTo("Get");
    }

    @Test
    void getProduct_wrongSupplier_throwsNotFound() {
        var created = stockService.createProduct(supplierId, new ProductCreateRequest("Get", "GET-002", null, null,
                new BigDecimal("10.00"), "TND", 10, 1));
        assertThatThrownBy(() -> stockService.getProduct(UUID.fromString("00000000-0000-0000-0000-000000000099"), created.id()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateProduct_updatesFields() {
        var created = stockService.createProduct(supplierId, new ProductCreateRequest("Upd", "UPD-001", null, null,
                new BigDecimal("10.00"), "TND", 10, 1));
        var request = new ProductUpdateRequest("Updated Name", null, null, new BigDecimal("15.00"), 20, null, null);
        var updated = stockService.updateProduct(supplierId, created.id(), request);
        assertThat(updated.name()).isEqualTo("Updated Name");
        assertThat(updated.unitPrice()).isEqualByComparingTo(new BigDecimal("15.00"));
    }

    @Test
    void deleteProduct_noReservations_setsInactive() {
        var created = stockService.createProduct(supplierId, new ProductCreateRequest("Del", "DEL-001", null, null,
                new BigDecimal("10.00"), "TND", 10, 1));
        var deleted = stockService.deleteProduct(supplierId, created.id());
        assertThat(deleted.status()).isEqualTo("INACTIVE");
    }

    @Test
    void createMovement_increasesQuantity() {
        var created = stockService.createProduct(supplierId, new ProductCreateRequest("Mov", "MOV-001", null, null,
                new BigDecimal("10.00"), "TND", 50, 1));
        var movement = stockService.createMovement(supplierId,
                new StockMovementRequest(created.id(), "IN", 20, "Restock", null));
        assertThat(movement.quantity()).isEqualTo(20);
    }

    @Test
    void createMovement_decreasesQuantity() {
        var created = stockService.createProduct(supplierId, new ProductCreateRequest("Mov2", "MOV-002", null, null,
                new BigDecimal("10.00"), "TND", 50, 1));
        stockService.createMovement(supplierId,
                new StockMovementRequest(created.id(), "OUT", 10, "Sale", null));
        var product = products.findById(created.id()).orElseThrow();
        assertThat(product.getQuantity()).isEqualTo(40);
    }

    @Test
    void createMovement_insufficientStock_throws() {
        var created = stockService.createProduct(supplierId, new ProductCreateRequest("Mov3", "MOV-003", null, null,
                new BigDecimal("10.00"), "TND", 5, 1));
        assertThatThrownBy(() -> stockService.createMovement(supplierId,
                new StockMovementRequest(created.id(), "OUT", 10, "Sale", null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("insuffisant");
    }

    @Test
    void createMovement_adjustment_setsQuantity() {
        var created = stockService.createProduct(supplierId, new ProductCreateRequest("Mov4", "MOV-004", null, null,
                new BigDecimal("10.00"), "TND", 50, 1));
        stockService.createMovement(supplierId,
                new StockMovementRequest(created.id(), "ADJUSTMENT", 75, "Count", null));
        var product = products.findById(created.id()).orElseThrow();
        assertThat(product.getQuantity()).isEqualTo(75);
    }

    @Test
    void createMovement_invalidType_throws() {
        var created = stockService.createProduct(supplierId, new ProductCreateRequest("Mov5", "MOV-005", null, null,
                new BigDecimal("10.00"), "TND", 50, 1));
        assertThatThrownBy(() -> stockService.createMovement(supplierId,
                new StockMovementRequest(created.id(), "INVALID", 10, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("inconnu");
    }

    @Test
    void createMovement_zeroQuantity_throws() {
        var created = stockService.createProduct(supplierId, new ProductCreateRequest("Mov6", "MOV-006", null, null,
                new BigDecimal("10.00"), "TND", 50, 1));
        assertThatThrownBy(() -> stockService.createMovement(supplierId,
                new StockMovementRequest(created.id(), "IN", 0, null, null)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("supérieure à 0");
    }
}
