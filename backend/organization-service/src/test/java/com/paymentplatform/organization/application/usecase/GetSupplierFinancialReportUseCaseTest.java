package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.CreateOrderRequest;
import com.paymentplatform.organization.application.dto.CreateOrganizationRequest;
import com.paymentplatform.organization.application.dto.CreateRelationRequest;
import com.paymentplatform.organization.application.dto.OrderItemRequest;
import com.paymentplatform.organization.application.dto.SupplierFinancialReportResponse;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.organization.infrastructure.http.PaymentSummaryClient;
import com.paymentplatform.shared.domain.exception.DomainException;
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
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@DisplayName("Rapport financier fournisseur : agrégation réelle sur H2, sans mock.")
class GetSupplierFinancialReportUseCaseTest {

    private static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @Autowired private CreateOrganizationUseCase createOrg;
    @Autowired private SupplierShopRelationUseCase relationUseCase;
    @Autowired private CreateOrderUseCase createOrder;
    @Autowired private ProductRepository products;
    @Autowired private OrderRepository orders;
    @Autowired private EntityManager entityManager;
    @Autowired private JdbcClient jdbcClient;

    private UUID supplierId;
    private UUID shopId;
    private UUID productAId;
    private UUID productBId;
    private UUID productCId;
    private GetSupplierFinancialReportUseCase useCase;

    @BeforeEach
    void setUp() {
        var supplier = createOrg.execute(new CreateOrganizationRequest("Rapport Supplier", "SUPPLIER",
                "1234567A/A/M/000", "PAT-RPT", "Adresse Test, Tunis",
                null, null, null, null, null, null), ACTOR_ID);
        supplierId = supplier.id();

        var shop = createOrg.execute(new CreateOrganizationRequest("Rapport Shop", "SHOP",
                "7654321B/A/M/000", "PAT-RPT", "Adresse Test, Tunis",
                null, null, null, null, null, null), ACTOR_ID);
        shopId = shop.id();

        relationUseCase.createRelation(new CreateRelationRequest(supplierId, shopId));

        productAId = createProduct("Produit A", "30.00");
        productBId = createProduct("Produit B", "25.00");
        productCId = createProduct("Produit C", "20.00");

        useCase = new GetSupplierFinancialReportUseCase(orders,
                new FixedPaymentSummaryClient(new SupplierFinancialReportResponse.PaymentSummary(
                        new BigDecimal("500.00"), 4, new BigDecimal("80.00"), 2,
                        BigDecimal.ZERO, 0)));
    }

    private UUID createProduct(String name, String unitPrice) {
        Product product = new Product();
        product.setSupplierId(supplierId);
        product.setName(name);
        product.setSku(name.replace(" ", "-") + "-" + System.nanoTime());
        product.setUnitPrice(new BigDecimal(unitPrice));
        product.setCurrency("TND");
        product.setQuantity(100);
        product.setMinQuantity(10);
        product.setReservedQty(0);
        return products.save(product).getId();
    }

    private UUID createOrder(UUID productId, int quantity) {
        var request = new CreateOrderRequest(supplierId, shopId, false, null, "TND", null,
                "Rapport financier", List.of(new OrderItemRequest(productId, quantity, null)));
        return createOrder.execute(request, ACTOR_ID, "SHOP").id();
    }

    private void updateOrder(UUID orderId, String createdAt, String status, String total) {
        orders.flush();
        int updated = jdbcClient.sql("UPDATE orders SET created_at = :createdAt, status = :status, total = :total WHERE id = :id")
                .param("createdAt", Timestamp.from(Instant.parse(createdAt)))
                .param("status", status)
                .param("total", new BigDecimal(total))
                .param("id", orderId)
                .update();
        assertThat(updated).isEqualTo(1);
        entityManager.clear();
    }

    @Test
    @DisplayName("Agrège la période, la période précédente, les statuts et les meilleurs produits.")
    void aggregatesPeriodSeriesGrowthAndStatuses() {
        updateOrder(createOrder(productAId, 4), "2026-10-01T10:00:00Z", "ACCEPTED", "100.00");
        updateOrder(createOrder(productCId, 2), "2026-10-02T10:00:00Z", "REJECTED", "30.50");
        updateOrder(createOrder(productBId, 2), "2026-10-04T10:00:00Z", "DRAFT", "50.00");
        updateOrder(createOrder(productCId, 1), "2026-10-04T12:00:00Z", "DELIVERED", "20.00");
        updateOrder(createOrder(productAId, 1), "2026-09-25T10:00:00Z", "ACCEPTED", "90.00");

        SupplierFinancialReportResponse report = useCase.execute(
                supplierId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7), "DAY");

        assertThat(report.from()).isEqualTo("2026-10-01");
        assertThat(report.to()).isEqualTo("2026-10-07");
        assertThat(report.previousFrom()).isEqualTo("2026-09-24");
        assertThat(report.previousTo()).isEqualTo("2026-09-30");
        assertThat(report.granularity()).isEqualTo("DAY");

        assertThat(report.summary().revenue()).isEqualByComparingTo("170.00");
        assertThat(report.summary().orderCount()).isEqualTo(3);
        assertThat(report.summary().averageBasket()).isEqualByComparingTo("56.67");
        assertThat(report.summary().openOrders()).isEqualTo(1);
        assertThat(report.summary().deliveredOrders()).isEqualTo(2);

        assertThat(report.previousSummary().revenue()).isEqualByComparingTo("90.00");
        assertThat(report.previousSummary().orderCount()).isEqualTo(1);
        assertThat(report.previousSummary().averageBasket()).isEqualByComparingTo("90.00");

        assertThat(report.series()).hasSize(7);
        assertThat(report.series().get(0).bucket()).isEqualTo("2026-10-01");
        assertThat(report.series().get(0).revenue()).isEqualByComparingTo("100.00");
        assertThat(report.series().get(3).bucket()).isEqualTo("2026-10-04");
        assertThat(report.series().get(3).orderCount()).isEqualTo(2);
        assertThat(report.series().get(3).revenue()).isEqualByComparingTo("70.00");
        assertThat(report.series().get(6).revenue()).isEqualByComparingTo("0");

        assertThat(report.previousSeries()).hasSize(7);
        assertThat(report.previousSeries().get(1).bucket()).isEqualTo("2026-09-25");
        assertThat(report.previousSeries().get(1).revenue()).isEqualByComparingTo("90.00");

        assertThat(report.orderCountByStatus())
                .containsEntry("ACCEPTED", 1L)
                .containsEntry("REJECTED", 1L)
                .containsEntry("DRAFT", 1L)
                .containsEntry("DELIVERED", 1L);

        assertThat(report.topProducts()).hasSize(3);
        assertThat(report.topProducts().get(0).productName()).isEqualTo("Produit A");
        assertThat(report.topProducts().get(0).totalQuantity()).isEqualTo(4);
        assertThat(report.topProducts().get(0).totalRevenue()).isEqualByComparingTo("120.00");
        assertThat(report.topProducts().get(1).productName()).isEqualTo("Produit B");
        assertThat(report.topProducts().get(1).totalQuantity()).isEqualTo(2);
        assertThat(report.topProducts().get(1).totalRevenue()).isEqualByComparingTo("50.00");
        assertThat(report.topProducts().get(2).productName()).isEqualTo("Produit C");
        assertThat(report.topProducts().get(2).totalQuantity()).isEqualTo(1);
        assertThat(report.topProducts().get(2).totalRevenue()).isEqualByComparingTo("20.00");

        assertThat(report.paymentSummary().confirmedTotal()).isEqualByComparingTo("500.00");
        assertThat(report.paymentSummary().confirmedCount()).isEqualTo(4);
        assertThat(report.paymentSummary().pendingTotal()).isEqualByComparingTo("80.00");
        assertThat(report.paymentSummary().pendingCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("Génère les seaux semaine et mois avec les périodes vides.")
    void buildsWeekAndMonthBucketsWithEmptyPeriodsFilled() {
        UUID emptySupplierId = UUID.randomUUID();

        SupplierFinancialReportResponse weeks = useCase.execute(
                emptySupplierId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 21), "WEEK");
        assertThat(weeks.series()).extracting(SupplierFinancialReportResponse.SeriesPoint::bucket)
                .containsExactly("2026-09-28", "2026-10-05", "2026-10-12", "2026-10-19");
        assertThat(weeks.series()).allSatisfy(point -> {
            assertThat(point.orderCount()).isZero();
            assertThat(point.revenue()).isEqualByComparingTo("0");
        });

        SupplierFinancialReportResponse months = useCase.execute(
                emptySupplierId, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 2), "MONTH");
        assertThat(months.series()).extracting(SupplierFinancialReportResponse.SeriesPoint::bucket)
                .containsExactly("2026-01", "2026-02", "2026-03");
    }

    @Test
    @DisplayName("Refuse une plage inversée et une granularité inconnue.")
    void rejectsInvalidRangeAndGranularity() {
        assertThatThrownBy(() -> useCase.execute(
                supplierId, LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 1), "DAY"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("date de");

        assertThatThrownBy(() -> useCase.execute(
                supplierId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), "QUARTER"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Granularit");
    }

    private static final class FixedPaymentSummaryClient extends PaymentSummaryClient {

        private final SupplierFinancialReportResponse.PaymentSummary summary;

        private FixedPaymentSummaryClient(SupplierFinancialReportResponse.PaymentSummary summary) {
            this.summary = summary;
        }

        @Override
        public SupplierFinancialReportResponse.PaymentSummary getSupplierPaymentSummary(UUID supplierId) {
            return summary;
        }
    }
}
