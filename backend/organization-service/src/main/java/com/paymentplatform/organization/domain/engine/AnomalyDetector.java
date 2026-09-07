package com.paymentplatform.organization.domain.engine;

import java.util.*;

/**
 * Anomaly detection for stock data.
 * Detects outliers, dormant products, negative stock, abnormal consumption.
 */
public class AnomalyDetector {

    public enum AnomalyType {
        NEGATIVE_STOCK, HIGH_CONSUMPTION, LOW_CONSUMPTION, DORMANT_PRODUCT,
        EXCESS_COVERAGE, DEMAND_SPIKE, DEMAND_DROP, LEAD_TIME_ANOMALY
    }

    public record Anomaly(
        AnomalyType type,
        String severity,    // LOW, MEDIUM, HIGH
        String description,
        double detectedValue,
        double expectedRange,
        String explanation
    ) {}

    /**
     * Detect anomalies in product stock data.
     */
    public List<Anomaly> detect(
            double currentStock,
            double reservedQty,
            double minQuantity,
            double avgDailyDemand,
            double demandStdDev,
            double leadTimeDays,
            double[] recentDemand,      // last 30 days
            double[] allDemandHistory) {

        List<Anomaly> anomalies = new ArrayList<>();

        // Negative stock
        if (currentStock < 0) {
            anomalies.add(new Anomaly(AnomalyType.NEGATIVE_STOCK, "HIGH",
                "Negative stock detected", currentStock, 0,
                "Stock is " + currentStock + " which is physically impossible. Data correction needed."));
        }

        // Dormant product
        if (recentDemand != null && allDemandHistory != null) {
            double recentSum = Arrays.stream(recentDemand).sum();
            double historicalAvg = Arrays.stream(allDemandHistory).average().orElse(0);

            if (recentSum == 0 && historicalAvg > 0) {
                anomalies.add(new Anomaly(AnomalyType.DORMANT_PRODUCT, "MEDIUM",
                    "No demand in recent period", recentSum, historicalAvg,
                    "Product had average demand of " + String.format("%.1f", historicalAvg) +
                    " but zero demand recently. May be obsolete."));
            }

            // Demand spike
            if (recentDemand.length > 0) {
                double recentAvg = Arrays.stream(recentDemand).average().orElse(0);
                if (historicalAvg > 0 && recentAvg > historicalAvg * 3) {
                    anomalies.add(new Anomaly(AnomalyType.DEMAND_SPIKE, "HIGH",
                        "Unusual demand spike", recentAvg, historicalAvg,
                        "Recent demand (" + String.format("%.1f", recentAvg) +
                        ") is " + String.format("%.1fx", recentAvg / historicalAvg) +
                        " higher than historical average. Verify if this is a real demand change."));
                }

                // Demand drop
                if (historicalAvg > 0 && recentAvg < historicalAvg * 0.2 && recentSum > 0) {
                    anomalies.add(new Anomaly(AnomalyType.DEMAND_DROP, "MEDIUM",
                        "Significant demand decrease", recentAvg, historicalAvg,
                        "Recent demand dropped to " + String.format("%.0f%%", (recentAvg / historicalAvg) * 100) +
                        " of historical average."));
                }
            }
        }

        // Excess coverage
        if (avgDailyDemand > 0) {
            double daysCover = currentStock / avgDailyDemand;
            if (daysCover > 365) {
                anomalies.add(new Anomaly(AnomalyType.EXCESS_COVERAGE, "HIGH",
                    "Over 1 year of stock coverage", daysCover, 30,
                    "Current stock covers " + String.format("%.0f", daysCover) +
                    " days of demand. Risk of obsolescence."));
            } else if (daysCover > 90) {
                anomalies.add(new Anomaly(AnomalyType.EXCESS_COVERAGE, "MEDIUM",
                    "Over 3 months of stock coverage", daysCover, 30,
                    "Stock coverage is " + String.format("%.0f", daysCover) + " days."));
            }
        }

        // High consumption (recent demand >> historical)
        if (recentDemand != null && demandStdDev > 0 && avgDailyDemand > 0) {
            for (int i = 0; i < recentDemand.length; i++) {
                double z = (recentDemand[i] - avgDailyDemand) / demandStdDev;
                if (z > 3) {
                    anomalies.add(new Anomaly(AnomalyType.HIGH_CONSUMPTION, "MEDIUM",
                        "Abnormal consumption spike on day " + (i + 1), recentDemand[i], avgDailyDemand,
                        "Consumption was " + String.format("%.1f", recentDemand[i]) +
                        " (Z-score: " + String.format("%.2f", z) + "). Investigate cause."));
                    break;
                }
            }
        }

        return anomalies;
    }

    /**
     * Calculate stock health score (0-100).
     */
    public double healthScore(List<Anomaly> anomalies) {
        double score = 100;
        for (Anomaly a : anomalies) {
            score -= switch (a.severity) {
                case "HIGH" -> 20;
                case "MEDIUM" -> 10;
                default -> 5;
            };
        }
        return Math.max(0, Math.min(100, score));
    }
}
