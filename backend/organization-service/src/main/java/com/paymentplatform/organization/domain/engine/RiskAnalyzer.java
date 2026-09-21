package com.paymentplatform.organization.domain.engine;

/**
 * Risk analyzer for stockout and overstock situations.
 * Uses probabilistic methods based on demand and lead time variability.
 */
public class RiskAnalyzer {

    public record RiskResult(
        double stockoutRisk,      // 0-1 probability
        double overstockRisk,     // 0-1 indicator
        double daysOfSupply,
        double weeksOfSupply,
        double turnoverRate,
        String stockoutLevel,     // LOW, MEDIUM, HIGH, CRITICAL
        String overstockLevel,
        String explanation
    ) {}

    /**
     * Calculate comprehensive risk assessment.
     */
    public RiskResult analyze(
            double currentStock,
            double reservedQty,
            double avgDailyDemand,
            double demandStdDev,
            double leadTimeDays,
            double leadTimeStdDev,
            double safetyStock,
            double unitCost,
            double annualDemand) {

        double availableStock = currentStock - reservedQty;
        double daysOfSupply = avgDailyDemand > 0 ? availableStock / avgDailyDemand : Double.MAX_VALUE;
        double weeksOfSupply = daysOfSupply / 7;

        // Stockout risk: probability that demand during lead time exceeds available stock + safety stock
        double stockoutRisk = calculateStockoutRisk(
            availableStock + safetyStock, avgDailyDemand, demandStdDev, leadTimeDays, leadTimeStdDev
        );

        // Overstock risk: based on days of supply vs reasonable range
        double overstockRisk = calculateOverstockRisk(daysOfSupply, avgDailyDemand, demandStdDev);

        // Turnover rate
        double avgInventory = annualDemand > 0 ? (availableStock + safetyStock) / 2 : availableStock;
        double turnoverRate = avgInventory > 0 ? annualDemand / avgInventory : 0;

        // Levels
        String stockoutLevel = riskLevel(stockoutRisk);
        String overstockLevel = overstockRisk > 0.7 ? "HIGH" : overstockRisk > 0.4 ? "MEDIUM" : "LOW";

        String explanation = String.format(
            "Stockout risk: %.1f%% (%s) | Overstock risk: %.1f%% (%s) | " +
            "Days of supply: %.1f | Turnover: %.1f/year",
            stockoutRisk * 100, stockoutLevel,
            overstockRisk * 100, overstockLevel,
            daysOfSupply, turnoverRate
        );

        return new RiskResult(
            stockoutRisk, overstockRisk, daysOfSupply, weeksOfSupply,
            turnoverRate, stockoutLevel, overstockLevel, explanation
        );
    }

    private double calculateStockoutRisk(double coverage, double avgD, double dStd, double lt, double ltStd) {
        if (avgD <= 0 || coverage <= 0) return 0;

        // Demand during lead time: mean = d * L, variance = L * σd² + d² * σL²
        double meanDLT = avgD * lt;
        double varDLT = lt * dStd * dStd + avgD * avgD * ltStd * ltStd;
        double stdDLT = Math.sqrt(varDLT);

        if (stdDLT <= 0) return coverage < meanDLT ? 1.0 : 0.0;

        // P(X > coverage) where X ~ N(meanDLT, stdDLT²)
        double z = (coverage - meanDLT) / stdDLT;
        return 1.0 - normalCDF(z);
    }

    private double calculateOverstockRisk(double daysOfSupply, double avgD, double dStd) {
        if (avgD <= 0) return daysOfSupply > 180 ? 0.9 : daysOfSupply > 90 ? 0.5 : 0.1;

        double cv = dStd / avgD;
        double targetDays = 30 + cv * 30; // Dynamic target based on variability
        double ratio = daysOfSupply / targetDays;

        if (ratio > 3) return 0.9;
        if (ratio > 2) return 0.7;
        if (ratio > 1.5) return 0.5;
        if (ratio > 1) return 0.3;
        return 0.1;
    }

    private String riskLevel(double risk) {
        if (risk >= 0.5) return "CRITICAL";
        if (risk >= 0.2) return "HIGH";
        if (risk >= 0.1) return "MEDIUM";
        return "LOW";
    }

    private static double normalCDF(double x) {
        double a1 = 0.254829592, a2 = -0.284496736, a3 = 1.421413741;
        double a4 = -1.453152027, a5 = 1.061405429, p = 0.3275911;
        int sign = x < 0 ? -1 : 1;
        x = Math.abs(x) / Math.sqrt(2);
        double t = 1.0 / (1.0 + p * x);
        double y = 1.0 - (((((a5 * t + a4) * t) + a3) * t + a2) * t + a1) * t * Math.exp(-x * x);
        return 0.5 * (1.0 + sign * y);
    }
}
