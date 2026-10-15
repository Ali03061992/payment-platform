package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.organization.domain.repository.WithholdingCertificateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Certificats de retenue rattachés aux FACTURES DE COMMANDES : émission à la
 * livraison, montants TTC, prix TTC sans TVA ajoutée.
 * SpringBootTest, profil "test" (H2).
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderWithholdingH2Test {

    @Autowired private CreateOrganizationUseCase createOrg;
    @Autowired private SupplierShopRelationUseCase relationUseCase;
    @Autowired private CreateOrderUseCase createOrder;
    @Autowired private ConfirmOrderUseCase confirmOrder;
    @Autowired private PrepareOrderUseCase prepareOrder;
    @Autowired private AcceptDeliveryUseCase acceptDelivery;
    @Autowired private ConfirmDeliveryUseCase confirmDelivery;
    @Autowired private DeliverOrderUseCase deliverOrder;
    @Autowired private AcceptOrderUseCase acceptOrder;
    @Autowired private ProductRepository products;
    @Autowired private com.paymentplatform.organization.domain.repository.OrderRepository orderRepository;
    @Autowired private WithholdingCertificateRepository certificates;

    private UUID supplierId;
    private UUID shopId;
    private final UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @BeforeEach
    void setUp() {
        var supplier = createOrg.execute(new CreateOrganizationRequest("W Supplier", "SUPPLIER",
                "1234567A/A/M/000", "PAT-TEST", "Adresse Test, Tunis", null, null, null, null, null, null), actor);
        supplierId = supplier.id();
        var shop = createOrg.execute(new CreateOrganizationRequest("W Shop", "SHOP",
                "7654321B/A/M/000", null, "Adresse Test, Tunis", null, null, null, null, null, null), actor);
        shopId = shop.id();
        relationUseCase.createRelation(
                new com.paymentplatform.organization.application.dto.CreateRelationRequest(supplierId, shopId));
    }

    private UUID product(String sku, String price, String vatRate, boolean ttc) {
        Product product = new Product();
        product.setSupplierId(supplierId);
        product.setName("Prod " + sku);
        product.setSku(sku);
        product.setUnitPrice(new BigDecimal(price));
        product.setCurrency("TND");
        product.setQuantity(100);
        product.setMinQuantity(1);
        product.setReservedQty(0);
        product.setVatRate(new BigDecimal(vatRate));
        product.setPriceTtc(ttc);
        return products.save(product).getId();
    }

    private UUID deliver(String price, String vatRate, boolean ttc, String nature) {
        UUID pid = product("W-" + System.nanoTime(), price, vatRate, ttc);
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
        if (nature != null) {
            return deliverOrder.execute(order.id(), actor, actor, nature).id();
        }
        return deliverOrder.execute(order.id(), actor, actor).id();
    }

    @Test
    void deliver_largeOrder_issuesReadyCertificate() {
        UUID orderId = deliver("10000", "19", false, null);
        var cert = certificates.findByOrderId(orderId);
        assertThat(cert).isPresent();
        assertThat(cert.get().status().name()).isEqualTo("READY");
        assertThat(cert.get().nature().name()).isEqualTo("MARCHANDISES");
        assertThat(cert.get().rate()).isEqualByComparingTo("1.5");
        // Assiette = TTC facture (10000 + 19%) : 1,5% de 11900.
        assertThat(cert.get().withholdingAmount()).isEqualByComparingTo("178.5000");
        assertThat(cert.get().netAmount()).isEqualByComparingTo("11721.5000");
        assertThat(cert.get().debtorMatricule()).isEqualTo("7654321B/A/M/000");
        assertThat(cert.get().beneficiaryMatricule()).isEqualTo("1234567A/A/M/000");
        assertThat(cert.get().number()).startsWith("CRT-");
    }

    @Test
    void deliver_smallOrder_noCertificate() {
        UUID orderId = deliver("500", "19", false, null);
        assertThat(certificates.findByOrderId(orderId)).isEmpty();
    }

    @Test
    void deliver_honoraires_applies3Percent() {
        UUID orderId = deliver("10000", "19", false, "HONORAIRES_REEL");
        var cert = certificates.findByOrderId(orderId).orElseThrow();
        assertThat(cert.withholdingAmount()).isEqualByComparingTo("357.0000");
    }

    @Test
    void priceTtc_noVatAdded() {
        UUID pid = product("TTC-" + System.nanoTime(), "1190.00", "19", true);
        var request = new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, null,
                List.of(new OrderItemRequest(pid, 1, null)));
        var response = createOrder.execute(request, actor, "SHOP");
        // 1190 TTC → HT 1000, TVA 190, TTC 1190 : rien n'est ajouté.
        assertThat(response.subtotal()).isEqualByComparingTo("1000.0000");
        assertThat(response.taxAmount()).isEqualByComparingTo("190.0000");
        assertThat(response.total()).isEqualByComparingTo("1190.0000");
        assertThat(response.items().get(0).priceTtc()).isTrue();
    }

    @Test
    void accept_withoutPriorCertificate_issuesOnCatchUp() {
        UUID pid = product("C-" + System.nanoTime(), "2000.00", "19", false);
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
        deliverOrder.execute(order.id(), actor, actor);
        assertThat(certificates.findByOrderId(order.id())).isPresent();
        acceptOrder.execute(order.id(), actor);
        assertThat(certificates.findByOrderId(order.id())).isPresent();
    }
}
