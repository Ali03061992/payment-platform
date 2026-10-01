package com.paymentplatform.organization.infrastructure.messaging;

import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.CreateRelationRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.application.usecase.CancelExpiredOrdersUseCase;
import com.paymentplatform.organization.application.usecase.CreateOrderUseCase;
import com.paymentplatform.organization.application.usecase.CreateOrganizationUseCase;
import com.paymentplatform.organization.application.usecase.SupplierShopRelationUseCase;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de CancelExpiredOrdersScheduler.
 * Périmètre : annulation automatique des commandes brouillon expirées via le planificateur.
 * Moyens : contexte SpringBootTest, base H2 réelle, antidatation via JdbcClient (zéro mock).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Planificateur d'annulation des expirés : traitement réel contre H2 sans mock.")
class CancelExpiredOrdersSchedulerTest {

    @Autowired
    private CancelExpiredOrdersScheduler scheduler;

    @Autowired
    private CancelExpiredOrdersUseCase cancelExpiredOrdersUseCase;

    @Autowired
    private CreateOrganizationUseCase createOrg;

    @Autowired
    private SupplierShopRelationUseCase relationUseCase;

    @Autowired
    private CreateOrderUseCase createOrder;

    @Autowired
    private ProductRepository products;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcClient jdbcClient;

    private UUID supplierId;
    private UUID shopId;
    private UUID productId;

    @BeforeEach
    void preparerDonnees() {
        var supplier = createOrg.execute(
                new CreateOrganizationRequest("Cancel Supplier", "SUPPLIER"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"));
        supplierId = supplier.id();
        var shop = createOrg.execute(
                new CreateOrganizationRequest("Cancel Shop", "SHOP"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"));
        shopId = shop.id();
        relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));

        Product product = new Product();
        product.setSupplierId(supplierId);
        product.setName("Produit annulation");
        product.setSku("CANCEL-" + System.nanoTime());
        product.setUnitPrice(new BigDecimal("12.00"));
        product.setCurrency("TND");
        product.setQuantity(100);
        product.setMinQuantity(10);
        product.setReservedQty(0);
        productId = products.save(product).getId();
    }

    private OrderResponse creerBrouillon() {
        var requete = new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, null, "Brouillon à expirer",
                List.of(new OrderItemRequest(productId, 2, null)));
        return createOrder.execute(requete, UUID.fromString("00000000-0000-0000-0000-000000000010"), "SHOP");
    }

    private void antidaterCreation(UUID orderId, long jours) {
        // Même discipline que pour delivered_at : flush, UPDATE brut, clear.
        orders.flush();
        Instant cible = Instant.now().minus(jours, ChronoUnit.DAYS);
        int lignes = jdbcClient.sql("UPDATE orders SET created_at = :cible WHERE id = :id")
                .param("cible", java.sql.Timestamp.from(cible))
                .param("id", orderId)
                .update();
        assertThat(lignes).isEqualTo(1);
        entityManager.clear();
    }

    @Test
    @DisplayName("Un brouillon vieux de plus de sept jours est annulé par le planificateur.")
    void cancelExpiredOrders_brouillonExpire_annuleCommande() {
        OrderResponse commande = creerBrouillon();
        antidaterCreation(commande.id(), 8);

        scheduler.cancelExpiredOrders();

        var annulee = orders.findById(commande.id()).orElseThrow();
        assertThat(annulee.getStatus()).isEqualTo("CANCELLED");
        assertThat(cancelExpiredOrdersUseCase).isNotNull();
    }

    @Test
    @DisplayName("Un brouillon récent est conservé après passage du planificateur.")
    void cancelExpiredOrders_brouillonRecent_conserveStatutDraft() {
        OrderResponse commande = creerBrouillon();

        scheduler.cancelExpiredOrders();

        var conservee = orders.findById(commande.id()).orElseThrow();
        assertThat(conservee.getStatus()).isEqualTo("DRAFT");
    }
}
