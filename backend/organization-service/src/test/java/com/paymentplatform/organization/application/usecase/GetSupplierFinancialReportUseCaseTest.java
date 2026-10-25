package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.SupplierFinancialReportResponse;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.infrastructure.http.PaymentSummaryClient;
import com.paymentplatform.shared.domain.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetSupplierFinancialReportUseCaseTest {

    private static final UUID SUPPLIER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentSummaryClient paymentSummaryClient;

    private GetSupplierFinancialReportUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new GetSupplierFinancialReportUseCase(orderRepository, paymentSummaryClient);
    }

    @Test
    void aggregatesPeriodSeriesGrowthAndStatuses() {
        List<Object[]> currentRows = List.<Object[]>of(
                row("2026-10-01T10:00:00Z", "100.00", "ACCEPTED"),
                row("2026-10-02T10:00:00Z", "30.50", "REJECTED"),
                row("2026-10-04T10:00:00Z", "50.00", "DRAFT"),
                row("2026-10-04T12:00:00Z", "20.00", "DELIVERED"));
        List<Object[]> previousRows = List.<Object[]>of(
                row("2026-09-25T10:00:00Z", "90.00", "ACCEPTED"));
        List<Object[]> topProducts = List.<Object[]>of(
                new Object[]{"Produit A", 4L, new BigDecimal("120.00")},
                new Object[]{"Produit B", 2L, new BigDecimal("50.00")});
        when(orderRepository.findOrderTotalsBySupplierAndPeriod(eq(SUPPLIER_ID), any(), any()))
                .thenReturn(currentRows, previousRows);
        when(orderRepository.topSoldProductsForSupplierBetween(eq(SUPPLIER_ID), any(), any(), anyInt()))
                .thenReturn(topProducts);
        when(paymentSummaryClient.getSupplierPaymentSummary(SUPPLIER_ID))
                .thenReturn(new SupplierFinancialReportResponse.PaymentSummary(
                        new BigDecimal("500.00"), 4, new BigDecimal("80.00"), 2,
                        BigDecimal.ZERO, 0));

        SupplierFinancialReportResponse report = useCase.execute(
                SUPPLIER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 7), "DAY");

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
        assertThat(report.topProducts()).hasSize(2);
        assertThat(report.topProducts().get(0).totalRevenue()).isEqualByComparingTo("120.00");
        assertThat(report.paymentSummary().confirmedCount()).isEqualTo(4);
        verify(orderRepository).topSoldProductsForSupplierBetween(eq(SUPPLIER_ID), any(), any(), eq(10));
    }

    @Test
    void buildsWeekAndMonthBucketsWithEmptyPeriodsFilled() {
        when(orderRepository.findOrderTotalsBySupplierAndPeriod(eq(SUPPLIER_ID), any(), any()))
                .thenReturn(List.of(), List.of());
        when(orderRepository.topSoldProductsForSupplierBetween(eq(SUPPLIER_ID), any(), any(), anyInt()))
                .thenReturn(List.of());
        when(paymentSummaryClient.getSupplierPaymentSummary(SUPPLIER_ID))
                .thenReturn(new SupplierFinancialReportResponse.PaymentSummary(
                        BigDecimal.ZERO, 0, BigDecimal.ZERO, 0, BigDecimal.ZERO, 0));

        SupplierFinancialReportResponse weeks = useCase.execute(
                SUPPLIER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 21), "WEEK");
        assertThat(weeks.series()).extracting(SupplierFinancialReportResponse.SeriesPoint::bucket)
                .containsExactly("2026-09-28", "2026-10-05", "2026-10-12", "2026-10-19");

        SupplierFinancialReportResponse months = useCase.execute(
                SUPPLIER_ID, LocalDate.of(2026, 1, 15), LocalDate.of(2026, 3, 2), "MONTH");
        assertThat(months.series()).extracting(SupplierFinancialReportResponse.SeriesPoint::bucket)
                .containsExactly("2026-01", "2026-02", "2026-03");
    }

    @Test
    void rejectsInvalidRangeAndGranularity() {
        assertThatThrownBy(() -> useCase.execute(
                SUPPLIER_ID, LocalDate.of(2026, 10, 8), LocalDate.of(2026, 10, 1), "DAY"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("début");

        assertThatThrownBy(() -> useCase.execute(
                SUPPLIER_ID, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 2), "QUARTER"))
                .isInstanceOf(DomainException.class)
                .hasMessageContaining("Granularité");

        verify(orderRepository, never()).findOrderTotalsBySupplierAndPeriod(any(), any(), any());
    }

    private Object[] row(String createdAt, String total, String status) {
        return new Object[]{Instant.parse(createdAt), new BigDecimal(total), status};
    }
}
