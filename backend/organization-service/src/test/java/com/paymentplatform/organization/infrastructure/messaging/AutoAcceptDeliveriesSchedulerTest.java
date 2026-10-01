package com.paymentplatform.organization.infrastructure.messaging;

import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.CreateRelationRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.application.usecase.AcceptDeliveryUseCase;
import com.paymentplatform.organization.application.usecase.AutoAcceptDeliveriesUseCase;
import com.paymentplatform.organization.application.usecase.ConfirmDeliveryUseCase;
import com.paymentplatform.organization.application.usecase.ConfirmOrderUseCase;
import com.paymentplatform.organization.application.usecase.CreateOrderUseCase;
import com.paymentplatform.organization.application.usecase.CreateOrganizationUseCase;
import com.paymentplatform.organization.application.usecase.DeliverOrderUseCase;
import com.paymentplatform.organization.application.usecase.PrepareOrderUseCase;
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
 * Tests de AutoAcceptDeliveriesScheduler.
 * Périmètre : acceptation automatique des livraisons sans réponse via le planificateur.
 * Moyens : contexte SpringBootTest, base H2 réelle, antidatation via JdbcClient (zéro mock).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Planificateur d'acceptation automatique : traitement réel contre H2 sans mock.")
class AutoAcceptDeliveriesSchedulerTest {

    @Autowired
    private AutoAcceptDeliveriesScheduler scheduler;

    @Autowired
    private AutoAcceptDeliveriesUseCase autoAcceptDeliveriesUseCase;

    @Autowired
    private CreateOrganizationUseCase createOrg;

    @Autowired
    private SupplierShopRelationUseCase relationUseCase;

    @Autowired
    private CreateOrderUseCase createOrder;

    @Autowired
    private ConfirmOrderUseCase confirmOrder;

    @Autowired
    private PrepareOrderUseCase prepareOrder;

    @Autowired
    private AcceptDeliveryUseCase acceptDelivery;

    @Autowired
    private ConfirmDeliveryUseCase confirmDelivery;

    @Autowired
    private DeliverOrderUseCase deliverOrder;

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
                new CreateOrganizationRequest("AutoAccept Supplier", "SUPPLIER"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"));
        supplierId = supplier.id();
        var shop = createOrg.execute(
                new CreateOrganizationRequest("AutoAccept Shop", "SHOP"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"));
        shopId = shop.id();
        relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));

        Product product = new Product();
        product.setSupplierId(supplierId);
        product.setName("Produit auto-accept");
        product.setSku("AUTO-" + System.nanoTime());
        product.setUnitPrice(new BigDecimal("10.00"));
        product.setCurrency("TND");
        product.setQuantity(100);
        product.setMinQuantity(10);
        product.setReservedQty(0);
        productId = products.save(product).getId();
    }

    private OrderResponse livrerCommande() {
        UUID acteur = UUID.fromString("00000000-0000-0000-0000-000000000010");
        UUID livreur = UUID.fromString("00000000-0000-0000-0000-000000000004");
        var requete = new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, null, "Commande scheduler",
                List.of(new OrderItemRequest(productId, 5, null)));
        OrderResponse commande = createOrder.execute(requete, acteur, "SHOP");
        confirmOrder.execute(commande.id(), acteur);
        prepareOrder.execute(commande.id(), acteur);
        prepareOrder.readyForDelivery(commande.id(), acteur);
        var entite = orders.findById(commande.id()).orElseThrow();
        entite.assignDeliveryAgent(livreur);
        orders.saveAndFlush(entite);
        acceptDelivery.execute(commande.id(), true, null, acteur);
        confirmDelivery.execute(commande.id(), java.time.LocalDate.now().plusDays(1), acteur);
        deliverOrder.execute(commande.id(), acteur, acteur);
        return commande;
    }

    private void antidaterLivraison(UUID orderId, long minutes) {
        // Flush d'abord les changements managés, puis UPDATE SQL brut, puis détache
        // le contexte pour que le planificateur relise l'état antidaté (sinon le
        // flush suivant écraserait la valeur avec l'entité managée périmée).
        orders.flush();
        Instant cible = Instant.now().minus(minutes, ChronoUnit.MINUTES);
        int lignes = jdbcClient.sql("UPDATE orders SET delivered_at = :cible WHERE id = :id")
                .param("cible", java.sql.Timestamp.from(cible))
                .param("id", orderId)
                .update();
        assertThat(lignes).isEqualTo(1);
        entityManager.clear();
    }

    @Test
    @DisplayName("Une livraison livrée depuis plus de 15 minutes est acceptée automatiquement.")
    void autoAcceptDeliveries_livraisonPerimee_accepteCommande() {
        OrderResponse commande = livrerCommande();
        antidaterLivraison(commande.id(), 16);

        scheduler.autoAcceptDeliveries();

        var acceptee = orders.findById(commande.id()).orElseThrow();
        assertThat(acceptee.getStatus()).isEqualTo("ACCEPTED");
        assertThat(autoAcceptDeliveriesUseCase).isNotNull();
    }

    @Test
    @DisplayName("Une livraison récente reste au statut livré après passage du planificateur.")
    void autoAcceptDeliveries_livraisonRecente_conserveStatutDelivered() {
        OrderResponse commande = livrerCommande();

        scheduler.autoAcceptDeliveries();

        var toujoursLivree = orders.findById(commande.id()).orElseThrow();
        assertThat(toujoursLivree.getStatus()).isEqualTo("DELIVERED");
    }
}
