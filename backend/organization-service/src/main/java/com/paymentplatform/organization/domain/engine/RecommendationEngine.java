package com.paymentplatform.organization.domain.engine;

import java.util.UUID;

import java.util.*;

/**
 * Recommendation engine that generates actionable stock management recommendations.
 * Combines all analysis results into clear, explainable recommendations.
 */
public class RecommendationEngine {

    public enum Action {
        ORDER_NOW, ORDER_SOON, MONITOR, REDUCE_STOCK, DISCONTINUE, RECLASSIFY, INVESTIGATE, NO_ACTION
    }

    public record Recommendation(
        UUID productId,
        String productName,
        Action action,
        String priority,       // URGENT, HIGH, MEDIUM, LOW
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
    ) {}

    /**
     * Generate recommendation for a single product.
     */
    public Recommendation generate(
            UUID productId,
            String productName,
            // Stock data
            double currentStock,
            double reservedQty,
            double minQuantity,
            // Classification
            String abcClass,
            String xyzClass,
            // Forecast
            String forecastMethod,
            double avgDailyDemand,
            // Safety stock
            double safetyStock,
            double zScore,
            // Reorder point
            double reorderPoint,
            boolean shouldReorder,
            // EOQ
            double orderQty,
            // Risk
            double stockoutRisk,
            double overstockRisk,
            double daysOfSupply,
            // Anomalies
            List<AnomalyDetector.Anomaly> anomalies) {

        List<String> factors = new ArrayList<>();
        Action action;
        String priority;
        String reasoning;

        // Determine action
        if (overstockRisk > 0.7 && daysOfSupply > 180) {
            action = Action.REDUCE_STOCK;
            priority = "HIGH";
            reasoning = "Significant overstock detected. Consider promotions, returns to supplier, or redistribution.";
            factors.add("Days of supply: " + String.format("%.0f", daysOfSupply));
            factors.add("Overstock risk: " + String.format("%.0f%%", overstockRisk * 100));
        } else if (stockoutRisk >= 0.5) {
            action = Action.ORDER_NOW;
            priority = "URGENT";
            reasoning = "Critical stockout risk. Immediate order required to prevent stockout.";
            factors.add("Stockout risk: " + String.format("%.0f%%", stockoutRisk * 100));
            factors.add("Current stock: " + String.format("%.0f", currentStock));
        } else if (shouldReorder) {
            action = Action.ORDER_SOON;
            priority = "HIGH";
            reasoning = String.format("Stock position (%.0f) is at or below reorder point (%.0f). Place order soon.",
                currentStock - reservedQty, reorderPoint);
            factors.add("Reorder point: " + String.format("%.0f", reorderPoint));
            factors.add("Available: " + String.format("%.0f", currentStock - reservedQty));
        } else if (stockoutRisk >= 0.2) {
            action = Action.MONITOR;
            priority = "MEDIUM";
            reasoning = "Moderate stockout risk. Monitor closely and prepare for reorder.";
            factors.add("Stockout risk: " + String.format("%.0f%%", stockoutRisk * 100));
        } else if (anomalies != null && anomalies.stream().anyMatch(a -> a.type() == AnomalyDetector.AnomalyType.DORMANT_PRODUCT)) {
            action = Action.INVESTIGATE;
            priority = "MEDIUM";
            reasoning = "Product shows no recent demand. Verify if still needed or mark for discontinuation.";
            factors.add("No recent demand activity");
        } else {
            action = Action.NO_ACTION;
            priority = "LOW";
            reasoning = "Stock levels are adequate. No immediate action required.";
            factors.add("Days of supply: " + String.format("%.0f", daysOfSupply));
            factors.add("Stockout risk: " + String.format("%.0f%%", stockoutRisk * 100));
        }

        // Add classification context
        factors.add("Classification: " + abcClass + xyzClass);
        factors.add("Forecast: " + forecastMethod);
        factors.add("Safety stock: " + String.format("%.0f", safetyStock));
        factors.add("Service level target: " + String.format("%.0f%%", zScoreToServiceLevel(zScore) * 100));

        return new Recommendation(
            productId, productName, action, priority,
            orderQty, reorderPoint, safetyStock, zScoreToServiceLevel(zScore),
            abcClass, xyzClass, abcClass + xyzClass,
            stockoutRisk, overstockRisk, forecastMethod,
            reasoning, factors
        );
    }

    private double zScoreToServiceLevel(double z) {
        return SafetyStockCalculator.zScoreFromServiceLevel(0.95);
    }

    /**
     * Generate summary of all recommendations.
     */
    public String generateSummary(List<Recommendation> recommendations) {
        long urgent = recommendations.stream().filter(r -> "URGENT".equals(r.priority())).count();
        long high = recommendations.stream().filter(r -> "HIGH".equals(r.priority()) && !"URGENT".equals(r.priority())).count();
        long orderNow = recommendations.stream().filter(r -> r.action() == Action.ORDER_NOW).count();
        long orderSoon = recommendations.stream().filter(r -> r.action() == Action.ORDER_SOON).count();
        long overstock = recommendations.stream().filter(r -> r.action() == Action.REDUCE_STOCK).count();

        return String.format(
            "Summary: %d products analyzed | %d urgent | %d high priority | " +
            "%d order now | %d order soon | %d overstock",
            recommendations.size(), urgent, high, orderNow, orderSoon, overstock
        );
    }
}
