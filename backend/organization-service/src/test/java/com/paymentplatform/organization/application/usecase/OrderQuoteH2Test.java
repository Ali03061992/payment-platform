package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.dto.SendQuoteRequest;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.OrderQuoteRepository;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Devis fournisseur : la boutique demande (sans remise), le fournisseur
 * modifie les lignes, pose les remises et envoie des devis versionnés.
 * SpringBootTest, profil "test" (H2).
 */
@DisplayName("Devis : chiffrage fournisseur versionné + remises.")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderQuoteH2Test {

    @Autowired private CreateOrganizationUseCase createOrg;
    @Autowired private SupplierShopRelationUseCase relationUseCase;
    @Autowired private CreateOrderUseCase createOrder;
    @Autowired private SendQuoteUseCase sendQuote;
    @Autowired private ProductRepository products;
    @Autowired private OrderQuoteRepository quotes;

    private UUID supplierId;
    private UUID shopId;
    private UUID productId;
    private final UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @BeforeEach
    void setUp() {
        var supplier = createOrg.execute(new CreateOrganizationRequest("Q Supplier", "SUPPLIER",
                "1234567A/A/M/000", "PAT-TEST", "Adresse Test, Tunis", null, null, null, null, null, null), actor);
        supplierId = supplier.id();
        var shop = createOrg.execute(new CreateOrganizationRequest("Q Shop", "SHOP",
                "7654321B/A/M/000", null, "Adresse Test, Tunis", null, null, null, null, null, null), actor);
        shopId = shop.id();
        relationUseCase.createRelation(
                new com.paymentplatform.organization.application.dto.CreateRelationRequest(supplierId, shopId));
        Product product = new Product();
        product.setSupplierId(supplierId);
        product.setName("Prod Q-" + System.nanoTime());
        product.setSku("Q-" + System.nanoTime());
        product.setUnitPrice(new BigDecimal("1000"));
        product.setCurrency("TND");
        product.setQuantity(100);
        product.setMinQuantity(1);
        product.setReservedQty(0);
        product.setVatRate(new BigDecimal("19"));
        productId = products.save(product).getId();
    }

    private UUID draftOrder() {
        var request = new CreateOrderRequest(supplierId, shopId, false, null, "TND", null, null, null,
                List.of(new OrderItemRequest(productId, 2, null)));
        return createOrder.execute(request, actor, "SHOP").id();
    }

    @Test
    @DisplayName("Premier devis : version 1, remise ligne + globale appliquées")
    void firstQuote_appliesDiscounts() {
        UUID orderId = draftOrder();
        var quote = sendQuote.execute(orderId,
                new SendQuoteRequest(List.of(new OrderItemRequest(productId, 2, new BigDecimal("10"))),
                        new BigDecimal("5")),
                supplierId, actor);
        assertThat(quote.version()).isEqualTo(1);
        assertThat(quote.globalDiscount()).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(quote.total()).isPositive();
        // 2 x 1000 = 2000 HT, -10 % ligne = 1800, -5 % global = 1710, TVA 19 % = 324.90, total 2034.90
        assertThat(quote.subtotal()).isEqualByComparingTo(new BigDecimal("1710.00"));
        assertThat(quote.total()).isEqualByComparingTo(new BigDecimal("2034.90"));
        assertThat(quote.linesJson()).contains("\"discountPct\":10");
        var history = quotes.findByOrderId(orderId);
        assertThat(history).hasSize(1);
    }

    @Test
    @DisplayName("Deuxième devis : version 2, historique conservé")
    void secondQuote_incrementsVersion() {
        UUID orderId = draftOrder();
        sendQuote.execute(orderId,
                new SendQuoteRequest(List.of(new OrderItemRequest(productId, 2, BigDecimal.ZERO)), null),
                supplierId, actor);
        var second = sendQuote.execute(orderId,
                new SendQuoteRequest(List.of(new OrderItemRequest(productId, 1, new BigDecimal("50"))), null),
                supplierId, actor);
        assertThat(second.version()).isEqualTo(2);
        assertThat(quotes.findByOrderId(orderId)).hasSize(2);
    }

    @Test
    @DisplayName("Boutique : envoi de devis refusé (périmètre fournisseur)")
    void shop_cannotSendQuote() {
        UUID orderId = draftOrder();
        assertThatThrownBy(() -> sendQuote.execute(orderId,
                new SendQuoteRequest(List.of(new OrderItemRequest(productId, 1, null)), null),
                shopId, actor))
                .hasMessageContaining("fournisseur");
    }

    @Test
    @DisplayName("Remise hors borne refusée")
    void outOfRangeDiscount_rejected() {
        UUID orderId = draftOrder();
        assertThatThrownBy(() -> sendQuote.execute(orderId,
                new SendQuoteRequest(List.of(new OrderItemRequest(productId, 1, new BigDecimal("120"))), null),
                supplierId, actor))
                .hasMessageContaining("0 et 100");
    }
}
