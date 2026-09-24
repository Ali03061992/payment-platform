package com.paymentplatform.organization.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record SupplierFinancialReportResponse(
        List<MonthlyRevenue> monthlyRevenue,
        Map<String, Long> orderCountByStatus,
        List<TopProduct> topProducts,
        PaymentSummary paymentSummary
) {
    public record MonthlyRevenue(
            String month,
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
