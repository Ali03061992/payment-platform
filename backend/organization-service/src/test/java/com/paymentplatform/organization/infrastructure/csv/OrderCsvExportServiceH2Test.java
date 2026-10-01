package com.paymentplatform.organization.infrastructure.csv;

import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.CreateRelationRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.dto.OrderResponse;
import com.paymentplatform.organization.application.usecase.CreateOrderUseCase;
import com.paymentplatform.organization.application.usecase.CreateOrganizationUseCase;
import com.paymentplatform.organization.application.usecase.SupplierShopRelationUseCase;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de OrderCsvExportService.
 * Périmètre : export CSV des commandes avec filtres.
 * Moyens : contexte SpringBootTest, base H2 réelle, vérification SQL via JdbcClient, zéro simulation.
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Export CSV des commandes : génération réelle contre H2 sans simulation.")
class OrderCsvExportServiceH2Test {

    @Autowired
    private OrderCsvExportService exportService;

    @Autowired
    private CreateOrganizationUseCase createOrg;

    @Autowired
    private SupplierShopRelationUseCase relationUseCase;

    @Autowired
    private CreateOrderUseCase createOrder;

    @Autowired
    private ProductRepository products;

    @Autowired
    private JdbcClient jdbcClient;

    private UUID supplierId;
    private UUID shopId;
    private UUID productId;

    @BeforeEach
    void preparerDonnees() {
        var supplier = createOrg.execute(
                new CreateOrganizationRequest("Export Supplier", "SUPPLIER"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"));
        supplierId = supplier.id();
        var shop = createOrg.execute(
                new CreateOrganizationRequest("Export Shop", "SHOP"),
                UUID.fromString("00000000-0000-0000-0000-000000000001"));
        shopId = shop.id();
        relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));

        Product product = new Product();
        product.setSupplierId(supplierId);
        product.setName("Produit export");
        product.setSku("EXP-" + System.nanoTime());
        product.setUnitPrice(new BigDecimal("20.00"));
        product.setCurrency("TND");
        product.setQuantity(50);
        product.setMinQuantity(5);
        product.setReservedQty(0);
        productId = products.save(product).getId();
    }

    private OrderResponse creerCommande() {
        var requete = new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, null, "Commande export",
                List.of(new OrderItemRequest(productId, 2, null)));
        return createOrder.execute(requete, UUID.fromString("00000000-0000-0000-0000-000000000010"), "SHOP");
    }

    @Test
    @DisplayName("L'export sans filtre contient l'en-tête et la commande créée.")
    void generateOrdersCsv_sansFiltre_contientCommande() {
        OrderResponse commande = creerCommande();

        String csv = exportService.generateOrdersCsv(null, null, null, null, null);

        assertThat(csv).contains("Reference,Supplier,Shop");
        assertThat(csv).contains(commande.reference());
        Integer lignes = jdbcClient.sql("SELECT COUNT(*) FROM orders").query(Integer.class).single();
        assertThat(lignes).isGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Le filtre par statut DRAFT ne retient que les brouillons.")
    void generateOrdersCsv_filtreStatut_neRetientQueBrouillons() {
        OrderResponse commande = creerCommande();

        String brouillons = exportService.generateOrdersCsv("DRAFT", null, null, null, null);
        String acceptes = exportService.generateOrdersCsv("ACCEPTED", null, null, null, null);

        assertThat(brouillons).contains(commande.reference());
        assertThat(acceptes).doesNotContain(commande.reference());
    }

    @Test
    @DisplayName("Le filtre par fournisseur inconnu ne retourne que l'en-tête.")
    void generateOrdersCsv_fournisseurInconnu_retourneEnteteSeule() {
        creerCommande();

        String csv = exportService.generateOrdersCsv(null, null, null, UUID.randomUUID(), null);

        assertThat(csv).contains("Reference,Supplier,Shop");
        assertThat(csv.lines().count()).isEqualTo(1L);
    }
}
