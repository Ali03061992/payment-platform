package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.organization.domain.repository.WithholdingCertificateRepository;
import com.paymentplatform.organization.infrastructure.messaging.WithholdingDueScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Synthèse mensuelle (réconciliation déclaration TEJ), recherche d'échéances
 * et scheduler de rappel J-7. SpringBootTest, profil "test" (H2).
 */
@DisplayName("Batch TEJ : synthèse mensuelle, échéances, rappel J-7.")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class WithholdingBatchSummaryH2Test {

    @Autowired private CreateOrganizationUseCase createOrg;
    @Autowired private SupplierShopRelationUseCase relationUseCase;
    @Autowired private CreateOrderUseCase createOrder;
    @Autowired private ConfirmOrderUseCase confirmOrder;
    @Autowired private PrepareOrderUseCase prepareOrder;
    @Autowired private AcceptDeliveryUseCase acceptDelivery;
    @Autowired private ConfirmDeliveryUseCase confirmDelivery;
    @Autowired private DeliverOrderUseCase deliverOrder;
    @Autowired private ProductRepository products;
    @Autowired private com.paymentplatform.organization.domain.repository.OrderRepository orderRepository;
    @Autowired private WithholdingCertificateRepository certificates;
    @Autowired private WithholdingBatchUseCase batch;
    @Autowired private WithholdingDueScheduler dueScheduler;

    private UUID supplierId;
    private UUID shopId;
    private final UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @BeforeEach
    void setUp() {
        var supplier = createOrg.execute(new CreateOrganizationRequest("S Supplier", "SUPPLIER",
                "1234567A/A/M/000", "PAT-TEST", "Adresse Test, Tunis", null, null, null, null, null, null), actor);
        supplierId = supplier.id();
        var shop = createOrg.execute(new CreateOrganizationRequest("S Shop", "SHOP",
                "7654321B/A/M/000", null, "Adresse Test, Tunis", null, null, null, null, null, null), actor);
        shopId = shop.id();
        relationUseCase.createRelation(
                new com.paymentplatform.organization.application.dto.CreateRelationRequest(supplierId, shopId));
    }

    private UUID deliver() {
        Product product = new Product();
        product.setSupplierId(supplierId);
        product.setName("Prod S-" + System.nanoTime());
        product.setSku("S-" + System.nanoTime());
        product.setUnitPrice(new BigDecimal("10000"));
        product.setCurrency("TND");
        product.setQuantity(100);
        product.setMinQuantity(1);
        product.setReservedQty(0);
        product.setVatRate(new BigDecimal("19"));
        UUID pid = products.save(product).getId();
        var request = new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, null,
                List.of(new OrderItemRequest(pid, 1, null)));
        var order = createOrder.execute(request, actor, "SHOP");
        confirmOrder.execute(order.id(), actor);
        prepareOrder.execute(order.id(), actor);
        prepareOrder.readyForDelivery(order.id(), actor);
        var entity = orderRepository.findById(order.id()).orElseThrow();
        entity.assignDeliveryAgent(UUID.fromString("00000000-0000-0000-0000-000000000004"));
        orderRepository.save(entity);
        acceptDelivery.execute(order.id(), true, null, actor);
        confirmDelivery.execute(order.id(), java.time.LocalDate.now().plusDays(1), actor);
        return deliverOrder.execute(order.id(), actor, actor).id();
    }

    @Test
    @DisplayName("Synthèse mensuelle : compteur, totaux, ventilation par nature")
    void monthlySummary_totalsByNature() {
        UUID orderId = deliver();
        var cert = certificates.findByOrderId(orderId).orElseThrow();
        YearMonth month = YearMonth.from(cert.orderDate());
        var summary = batch.monthlySummary(shopId, month, shopId, false);
        assertThat(summary.certificateCount()).isGreaterThanOrEqualTo(1);
        assertThat(summary.grossTotal()).isPositive();
        assertThat(summary.withholdingTotal()).isPositive();
        assertThat(summary.byNature()).containsKey(cert.nature().name());
        assertThat(summary.byNature().get(cert.nature().name())).isPositive();
    }

    @Test
    @DisplayName("findDueBefore : échéance lointaine trouvée, limite aujourd'hui vide")
    void findDueBefore_filtersByLimit() {
        deliver();
        assertThat(certificates.findDueBefore(LocalDate.now().plusMonths(3))).isNotEmpty();
        assertThat(certificates.findDueBefore(LocalDate.now())).isEmpty();
    }

    @Test
    @DisplayName("Scheduler J-7 : s'exécute sans erreur (cas nominal sans échéance proche)")
    void dueScheduler_runsCleanly() {
        deliver();
        dueScheduler.remindDueCertificates();
    }
}
