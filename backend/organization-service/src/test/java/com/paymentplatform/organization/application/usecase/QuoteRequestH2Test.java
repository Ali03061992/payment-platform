package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.CreateQuoteRequestRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.dto.SendQuoteRequest;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.model.QuoteRequestStatus;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.organization.domain.repository.QuoteRequestRepository;
import com.paymentplatform.organization.domain.repository.SupplierQuoteRepository;
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
 * Demandes de devis découplées : demande boutique (zéro remise), chiffrage
 * fournisseur versionné, acceptation créant la commande CONFIRMÉE.
 * SpringBootTest, profil "test" (H2).
 */
@DisplayName("Demandes de devis : demande, chiffrage, acceptation.")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class QuoteRequestH2Test {

    @Autowired private CreateOrganizationUseCase createOrg;
    @Autowired private SupplierShopRelationUseCase relationUseCase;
    @Autowired private CreateQuoteRequestUseCase createRequest;
    @Autowired private SendRequestQuoteUseCase sendQuote;
    @Autowired private AcceptQuoteUseCase acceptQuote;
    @Autowired private CancelQuoteRequestUseCase cancelRequest;
    @Autowired private ListQuoteRequestsUseCase listRequests;
    @Autowired private ProductRepository products;
    @Autowired private QuoteRequestRepository requests;
    @Autowired private SupplierQuoteRepository quotes;

    private UUID supplierId;
    private UUID shopId;
    private UUID productId;
    private final UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @BeforeEach
    void setUp() {
        var supplier = createOrg.execute(new CreateOrganizationRequest("D Supplier", "SUPPLIER",
                "1234567A/A/M/000", "PAT-TEST", "Adresse Test, Tunis", null, null, null, null, null, null), actor);
        supplierId = supplier.id();
        var shop = createOrg.execute(new CreateOrganizationRequest("D Shop", "SHOP",
                "7654321B/A/M/000", null, "Adresse Test, Tunis", null, null, null, null, null, null), actor);
        shopId = shop.id();
        relationUseCase.createRelation(
                new com.paymentplatform.organization.application.dto.CreateRelationRequest(supplierId, shopId));
        Product product = new Product();
        product.setSupplierId(supplierId);
        product.setName("Prod D-" + System.nanoTime());
        product.setSku("D-" + System.nanoTime());
        product.setUnitPrice(new BigDecimal("1000"));
        product.setCurrency("TND");
        product.setQuantity(100);
        product.setMinQuantity(1);
        product.setReservedQty(0);
        product.setVatRate(new BigDecimal("19"));
        productId = products.save(product).getId();
    }

    private UUID demande() {
        return createRequest.execute(
                new CreateQuoteRequestRequest(supplierId, "Besoin urgent",
                        List.of(new CreateQuoteRequestRequest.QuoteRequestLine(productId, 2))),
                shopId, actor).id();
    }

    @Test
    @DisplayName("Demande : statut PENDING, lignes sans remise, notifiée")
    void demande_pendingSansRemise() {
        var req = createRequest.execute(
                new CreateQuoteRequestRequest(supplierId, null,
                        List.of(new CreateQuoteRequestRequest.QuoteRequestLine(productId, 3))),
                shopId, actor);
        assertThat(req.reference()).startsWith("DEV-");
        assertThat(req.status()).isEqualTo(QuoteRequestStatus.PENDING.name());
        assertThat(req.linesJson()).contains("\"qty\":3");
        assertThat(req.linesJson()).doesNotContain("discount");
    }

    @Test
    @DisplayName("Chiffrage : version 1, remise ligne seule, demande QUOTED")
    void chiffrage_versionne() {
        UUID requestId = demande();
        var quote = sendQuote.execute(requestId,
                new SendQuoteRequest(List.of(new OrderItemRequest(productId, 2, new BigDecimal("10")))),
                supplierId, actor);
        assertThat(quote.version()).isEqualTo(1);
        assertThat(quote.subtotal()).isEqualByComparingTo(new BigDecimal("1800.00"));
        assertThat(quote.total()).isEqualByComparingTo(new BigDecimal("2142.00"));
        assertThat(requests.findById(requestId).orElseThrow().status())
                .isEqualTo(QuoteRequestStatus.QUOTED);
        assertThat(quotes.findByRequestId(requestId)).hasSize(1);
    }

    @Test
    @DisplayName("PDF devis : binaire valide, totaux du snapshot (HT 1800.00)")
    void pdfDevis_totauxSnapshot() {
        UUID requestId = demande();
        sendQuote.execute(requestId,
                new SendQuoteRequest(List.of(new OrderItemRequest(productId, 2, new BigDecimal("10")))),
                supplierId, actor);
        var domain = quotes.findByRequestId(requestId).get(0);
        assertThat(domain.linesJson()).contains("\"lineTotal\":1800.0000");
        byte[] pdf = new com.paymentplatform.organization.application.service.QuotePdfService()
                .generateQuotePdf(domain,
                        com.paymentplatform.organization.application.service.InvoicePdfService.InvoiceParty.named("Fournisseur"),
                        com.paymentplatform.organization.application.service.InvoicePdfService.InvoiceParty.named("Boutique"),
                        shopId, "Demande", "DEV-TEST");
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, java.nio.charset.StandardCharsets.UTF_8)).isEqualTo("%PDF");
    }

    @Test
    @DisplayName("Acceptation : commande CONFIRMÉE aux conditions du devis")
    void acceptation_creeCommandeConfirmee() {
        UUID requestId = demande();
        var quote = sendQuote.execute(requestId,
                new SendQuoteRequest(List.of(new OrderItemRequest(productId, 2, new BigDecimal("10")))),
                supplierId, actor);
        var order = acceptQuote.execute(quote.id(), shopId, actor);
        assertThat(order.status()).isEqualTo("CONFIRMED");
        assertThat(order.total()).isEqualByComparingTo(new BigDecimal("2142.00"));
        assertThat(requests.findById(requestId).orElseThrow().status())
                .isEqualTo(QuoteRequestStatus.ACCEPTED);
    }

    @Test
    @DisplayName("Boutique tierce : acceptation refusée")
    void acceptation_tiersRefusee() {
        UUID requestId = demande();
        var quote = sendQuote.execute(requestId,
                new SendQuoteRequest(List.of(new OrderItemRequest(productId, 1, null))),
                supplierId, actor);
        assertThatThrownBy(() -> acceptQuote.execute(quote.id(), UUID.randomUUID(), actor))
                .hasMessageContaining("boutique");
    }

    @Test
    @DisplayName("Listes : boutique voit les siennes, fournisseur les reçues")
    void listes_parRole() {
        demande();
        assertThat(listRequests.forShop(shopId)).hasSize(1);
        assertThat(listRequests.forSupplier(supplierId)).hasSize(1);
        assertThat(listRequests.forShop(UUID.randomUUID())).isEmpty();
    }

    @Test
    @DisplayName("Annulation boutique : acceptation ultérieure impossible")
    void annulation_bloqueAcceptation() {
        UUID requestId = demande();
        var cancelled = cancelRequest.execute(requestId, shopId);
        assertThat(cancelled.status()).isEqualTo(QuoteRequestStatus.CANCELLED.name());
        assertThatThrownBy(() -> sendQuote.execute(requestId,
                new SendQuoteRequest(List.of(new OrderItemRequest(productId, 1, null))),
                supplierId, actor))
                .hasMessageContaining("clôturée");
    }
}
