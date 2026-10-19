package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.CreateRelationRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.domain.model.InvoiceJournal;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.InvoiceJournalRepository;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.shared.domain.model.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Journal des factures : numérotation séquentielle par fournisseur et par année,
 * émission idempotente par commande, recherche scopée.
 * SpringBootTest, profil "test" (H2).
 */
@DisplayName("Journal des factures : séquence, idempotence, recherche.")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class InvoiceJournalH2Test {

    @Autowired private CreateOrganizationUseCase createOrg;
    @Autowired private CreateOrderUseCase createOrder;
    @Autowired private SupplierShopRelationUseCase relationUseCase;
    @Autowired private IssueInvoiceUseCase issueInvoice;
    @Autowired private InvoiceJournalRepository journal;
    @Autowired private OrderRepository orders;
    @Autowired private ProductRepository products;

    private UUID supplierId;
    private UUID shopId;
    private UUID otherSupplierId;
    private final UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @BeforeEach
    void setUp() {
        supplierId = createOrg.execute(new CreateOrganizationRequest("J Supplier", "SUPPLIER",
                "1234567A/A/M/000", "PAT-J", "Adresse Test, Tunis", null, null, null, null, null, null), actor).id();
        shopId = createOrg.execute(new CreateOrganizationRequest("J Shop", "SHOP",
                "7654321B/A/M/000", null, "Adresse Test, Tunis", null, null, null, null, null, null), actor).id();
        otherSupplierId = createOrg.execute(new CreateOrganizationRequest("J Supplier 2", "SUPPLIER",
                "1122334C/A/M/000", null, "Adresse Test, Tunis", null, null, null, null, null, null), actor).id();
        createProduct(supplierId, "J-PROD");
        createProduct(otherSupplierId, "J-PROD-2");
        relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));
        relationUseCase.createRelation(new CreateRelationRequest(otherSupplierId, shopId));
    }

    private void createProduct(UUID owner, String sku) {
        Product p = new Product();
        p.setSupplierId(owner);
        p.setName("Prod " + sku);
        p.setSku(sku + "-" + System.nanoTime());
        p.setUnitPrice(new BigDecimal("1000"));
        p.setCurrency("TND");
        p.setQuantity(100);
        products.save(p);
    }

    private UUID productIdOf(UUID owner) {
        return products.findAll().stream()
                .filter(p -> p.getSupplierId().equals(owner))
                .findFirst().orElseThrow().getId();
    }

    private UUID createReadyOrder(UUID supplier, UUID shop) {
        var order = createOrder.execute(new CreateOrderRequest(supplier, shop, false, null,
                "TND", null, "notes", List.of(new OrderItemRequest(productIdOf(supplier), 2, null))), actor, "SHOP_ADMIN");
        var o = orders.findById(order.id()).orElseThrow();
        o.confirm();
        o.prepare();
        o.readyForDelivery();
        return orders.save(o).getId();
    }

    private String expectedNumber(int seq) {
        return String.format("FA-%d-%06d", LocalDate.now().getYear(), seq);
    }

    @Test
    @DisplayName("Première émission : numéro FA-AAAA-000001 attribué")
    void issueIfNeeded_firstCall_assignsFirstNumber() {
        var order = orders.findById(createReadyOrder(supplierId, shopId)).orElseThrow();
        InvoiceJournal entry = issueInvoice.issueIfNeeded(order);
        assertThat(entry.invoiceNumber()).isEqualTo(expectedNumber(1));
        assertThat(entry.orderId()).isEqualTo(order.getId());
        assertThat(entry.supplierId()).isEqualTo(supplierId);
        assertThat(entry.shopId()).isEqualTo(shopId);
        assertThat(entry.totalTtc()).isEqualByComparingTo("2380");
        assertThat(entry.vatAmount()).isEqualByComparingTo("380");
    }

    @Test
    @DisplayName("Idempotence : la seconde émission réutilise le même numéro")
    void issueIfNeeded_secondCall_reusesSameNumber() {
        var order = orders.findById(createReadyOrder(supplierId, shopId)).orElseThrow();
        String first = issueInvoice.issueIfNeeded(order).invoiceNumber();
        String second = issueInvoice.issueIfNeeded(order).invoiceNumber();
        assertThat(second).isEqualTo(first);
        assertThat(journal.search(supplierId, null, null, 0, 10).totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Séquence : deux commandes du même fournisseur incrémentent le compteur")
    void issueIfNeeded_twoOrders_incrementsSequence() {
        var o1 = orders.findById(createReadyOrder(supplierId, shopId)).orElseThrow();
        var o2 = orders.findById(createReadyOrder(supplierId, shopId)).orElseThrow();
        assertThat(issueInvoice.issueIfNeeded(o1).invoiceNumber()).isEqualTo(expectedNumber(1));
        assertThat(issueInvoice.issueIfNeeded(o2).invoiceNumber()).isEqualTo(expectedNumber(2));
    }

    @Test
    @DisplayName("Séquences indépendantes par fournisseur")
    void issueIfNeeded_otherSupplier_hasOwnSequence() {
        var o1 = orders.findById(createReadyOrder(supplierId, shopId)).orElseThrow();
        var o2 = orders.findById(createReadyOrder(otherSupplierId, shopId)).orElseThrow();
        assertThat(issueInvoice.issueIfNeeded(o1).invoiceNumber()).isEqualTo(expectedNumber(1));
        assertThat(issueInvoice.issueIfNeeded(o2).invoiceNumber()).isEqualTo(expectedNumber(1));
    }

    @Test
    @DisplayName("Recherche scopée : le fournisseur ne voit que ses factures")
    void search_scopedBySupplier() {
        var o1 = orders.findById(createReadyOrder(supplierId, shopId)).orElseThrow();
        var o2 = orders.findById(createReadyOrder(otherSupplierId, shopId)).orElseThrow();
        issueInvoice.issueIfNeeded(o1);
        issueInvoice.issueIfNeeded(o2);
        PageResult<InvoiceJournal> scoped = journal.search(supplierId, null, null, 0, 10);
        assertThat(scoped.totalElements()).isEqualTo(1);
        assertThat(scoped.items().get(0).supplierId()).isEqualTo(supplierId);
        assertThat(journal.search(null, shopId, null, 0, 10).totalElements()).isEqualTo(2);
        assertThat(journal.search(null, null, LocalDate.now().getYear(), 0, 10).totalElements()).isEqualTo(2);
        assertThat(journal.search(null, null, LocalDate.now().getYear() - 1, 0, 10).totalElements()).isZero();
    }
}
