package com.paymentplatform.organization.application.usecase;

import com.paymentplatform.organization.application.dto.SupplierFinancialReportResponse;
import com.paymentplatform.organization.domain.repository.OrderRepository;
import com.paymentplatform.organization.infrastructure.http.PaymentSummaryClient;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GetSupplierFinancialReportUseCase {

    private final OrderRepository orderRepository;
    private final PaymentSummaryClient paymentSummaryClient;
    private static final DateTimeFormatter MONTH_FMT = DateTimeFormatter.ofPattern("yyyy-MM");

    public GetSupplierFinancialReportUseCase(OrderRepository orderRepository,
                                              PaymentSummaryClient paymentSummaryClient) {
        this.orderRepository = orderRepository;
        this.paymentSummaryClient = paymentSummaryClient;
    }

    public SupplierFinancialReportResponse execute(UUID supplierId) {
        Instant twelveMonthsAgo = LocalDate.now().minusMonths(12)
                .withDayOfMonth(1)
                .atStartOfDay()
                .toInstant(ZoneOffset.UTC);

        List<SupplierFinancialReportResponse.MonthlyRevenue> monthlyRevenue =
                buildMonthlyRevenue(orderRepository.monthlyRevenueBySupplier(supplierId, twelveMonthsAgo));

        Map<String, Long> orderCountByStatus =
                buildOrderCountByStatus(orderRepository.orderCountByStatusForSupplier(supplierId));

        List<SupplierFinancialReportResponse.TopProduct> topProducts =
                buildTopProducts(orderRepository.topSoldProductsForSupplier(supplierId, 10));

        SupplierFinancialReportResponse.PaymentSummary paymentSummary =
                paymentSummaryClient.getSupplierPaymentSummary(supplierId);

        return new SupplierFinancialReportResponse(monthlyRevenue, orderCountByStatus, topProducts, paymentSummary);
    }

    private List<SupplierFinancialReportResponse.MonthlyRevenue> buildMonthlyRevenue(List<Object[]> rows) {
        return rows.stream().map(row -> {
            String month = "unknown";
            if (row[0] != null) {
                if (row[0] instanceof java.sql.Timestamp ts) {
                    month = ts.toLocalDateTime().format(MONTH_FMT);
                } else {
                    month = row[0].toString().substring(0, 7);
                }
            }
            long orderCount = row[1] != null ? ((Number) row[1]).longValue() : 0;
            BigDecimal revenue = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            return new SupplierFinancialReportResponse.MonthlyRevenue(month, orderCount, revenue);
        }).toList();
    }

    private Map<String, Long> buildOrderCountByStatus(List<Object[]> rows) {
        Map<String, Long> result = new LinkedHashMap<>();
        for (Object[] row : rows) {
            String status = row[0] != null ? row[0].toString() : "UNKNOWN";
            long count = row[1] != null ? ((Number) row[1]).longValue() : 0;
            result.put(status, count);
        }
        return result;
    }

    private List<SupplierFinancialReportResponse.TopProduct> buildTopProducts(List<Object[]> rows) {
        return rows.stream().map(row -> {
            String name = row[0] != null ? row[0].toString() : "";
            long qty = row[1] != null ? ((Number) row[1]).longValue() : 0;
            BigDecimal revenue = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
            return new SupplierFinancialReportResponse.TopProduct(name, qty, revenue);
        }).toList();
    }
}
