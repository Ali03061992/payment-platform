package com.paymentplatform.organization.application.dto;

import com.paymentplatform.organization.domain.engine.AnomalyDetector;
import com.paymentplatform.organization.domain.engine.RecommendationEngine;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record StockOptimizationResponse(
    List<ProductOptimizationResult> products,
    Map<String, Long> abcDistribution,
    Map<String, Long> xyzDistribution,
    Map<String, Long> abcxzDistribution,
    Map<String, Long> actionDistribution,
    Map<String, Long> riskDistribution,
    double totalStockValue,
    double totalSafetyStockValue,
    int productsToOrder,
    int productsAtRisk,
    int productsOverstocked,
    String summary,
    Instant calculatedAt
) {
    public record ProductOptimizationResult(
        Long productId,
        String productName,
        String sku,
        String abcClass,
        double annualValue,
        int abcRank,
        String xyzClass,
        double coefficientOfVariation,
        String demandPattern,
        String combinedClass,
        String policyDescription,
        double currentStock,
        double reservedQty,
        double availableStock,
        double minQuantity,
        String forecastMethod,
        double avgDailyDemand,
        double forecastAccuracy,
        double[] forecastValues,
        double safetyStock,
        double zScore,
        double targetServiceLevel,
        String safetyStockFormula,
        double reorderPoint,
        boolean shouldReorder,
        double daysUntilReorder,
        double economicOrderQty,
        double adjustedOrderQty,
        double ordersPerYear,
        double totalAnnualCost,
        double stockoutRisk,
        double overstockRisk,
        double daysOfSupply,
        double turnoverRate,
        String stockoutLevel,
        List<AnomalyInfo> anomalies,
        double healthScore,
        RecommendationInfo recommendation,
        String explanation,
        Instant calculatedAt
    ) {}

    public record AnomalyInfo(
        String type,
        String severity,
        String description,
        double detectedValue,
        double expectedRange,
        String explanation
    ) {
        public static AnomalyInfo from(AnomalyDetector.Anomaly a) {
            return new AnomalyInfo(
                a.type().name(), a.severity(), a.description(),
                a.detectedValue(), a.expectedRange(), a.explanation()
            );
        }
    }

    public record RecommendationInfo(
        String action,
        String priority,
        double orderQty,
        double reorderPoint,
        double safetyStock,
        double targetServiceLevel,
        String abcClass,
        String xyzClass,
        String combinedClass,
        double stockoutRisk,
        double overstockRisk,
        String forecastMethod,
        String reasoning,
        List<String> keyFactors
    ) {
        public static RecommendationInfo from(RecommendationEngine.Recommendation r) {
            return new RecommendationInfo(
                r.action().name(), r.priority(), r.orderQty(), r.reorderPoint(),
                r.safetyStock(), r.targetServiceLevel(), r.abcClass(), r.xyzClass(),
                r.combinedClass(), r.stockoutRisk(), r.overstockRisk(),
                r.forecastMethod(), r.reasoning(), r.keyFactors()
            );
        }
    }
}
