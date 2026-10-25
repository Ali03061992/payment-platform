package com.paymentplatform.organization.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SupplierFinancialReportResponse(
        String from,
        String to,
        String previousFrom,
        String previousTo,
        String granularity,
        Summary summary,
        Summary previousSummary,
        List<SeriesPoint> series,
        List<SeriesPoint> previousSeries,
        Map<String, Long> orderCountByStatus,
        List<TopProduct> topProducts,
        PaymentSummary paymentSummary
) {
    public record Summary(
            BigDecimal revenue,
            long orderCount,
            BigDecimal averageBasket,
            long openOrders,
            long deliveredOrders
    ) {}

    public record SeriesPoint(
            String bucket,
            long orderCount,
            BigDecimal revenue
    ) {}

    public record TopProduct(
            String productName,
            long totalQuantity,
            BigDecimal totalRevenue
    ) {}

    public record PaymentSummary(
            BigDecimal confirmedTotal,
            long confirmedCount,
            BigDecimal pendingTotal,
            long pendingCount,
            BigDecimal rejectedTotal,
            long rejectedCount
    ) {}
}
