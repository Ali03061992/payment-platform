package com.paymentplatform.organization.domain.engine;

/**
 * Statistical safety stock calculator.
 * Uses Z-score from service level, demand variability, and lead time variability.
 * Never uses arbitrary percentages.
 */
public class SafetyStockCalculator {

    public record SafetyStockResult(
        double safetyStock,
        double zScore,
        double demandStdDev,
        double leadTime,
        double leadTimeStdDev,
        double demandVariabilityComponent,
        double leadTimeVariabilityComponent,
        double forecastUncertaintyComponent,
        String formula,
        String explanation
    ) {}

    /**
     * Calculate safety stock using the standard formula:
     * SS = Z * sqrt(L * σd² + d² * σL²)
     *
     * @param zScore z-score for target service level
     * @param avgDemand average demand per period
     * @param demandStdDev standard deviation of demand
     * @param leadTime average lead time (in same periods as demand)
     * @param leadTimeStdDev standard deviation of lead time
     * @param forecastStdDev forecast error standard deviation (optional, for uncertainty)
     */
    public SafetyStockResult calculate(
            double zScore,
            double avgDemand,
            double demandStdDev,
            double leadTime,
            double leadTimeStdDev,
            double forecastStdDev) {

        if (avgDemand <= 0 || leadTime <= 0) {
            return zeroResult(zScore, avgDemand, demandStdDev, leadTime, leadTimeStdDev,
                "No safety stock needed: zero demand or lead time");
        }

        // Demand variability component: Z * sqrt(L) * σd
        double demandComponent = zScore * Math.sqrt(leadTime) * demandStdDev;

        // Lead time variability component: Z * d * σL
        double ltComponent = zScore * avgDemand * leadTimeStdDev;

        // Forecast uncertainty component (if available)
        double forecastComponent = forecastStdDev > 0 ? zScore * Math.sqrt(leadTime) * forecastStdDev : 0;

        // Total safety stock
        double safetyStock = Math.sqrt(
            demandComponent * demandComponent + ltComponent * ltComponent + forecastComponent * forecastComponent
        );

        // Round up to nearest integer
        safetyStock = Math.ceil(safetyStock);

        String formula = String.format(
            "SS = Z(%.3f) * sqrt(L(%.1f) * σd²(%.3f) + d²(%.2f) * σL²(%.3f)) = %.0f",
            zScore, leadTime, demandStdDev * demandStdDev, avgDemand, leadTimeStdDev * leadTimeStdDev, safetyStock
        );

        String explanation = String.format(
            "Safety stock: %.0f units | Z=%.3f (service level) | Demand variability: %.1f%% | " +
            "Lead time: %.1f periods | LT variability: %.1f%%",
            safetyStock, zScore,
            avgDemand > 0 ? (demandStdDev / avgDemand) * 100 : 0,
            leadTime,
            leadTime > 0 ? (leadTimeStdDev / leadTime) * 100 : 0
        );

        return new SafetyStockResult(
            safetyStock, zScore, demandStdDev, leadTime, leadTimeStdDev,
            demandComponent, ltComponent, forecastComponent,
            formula, explanation
        );
    }

    /**
     * Calculate safety stock with default lead time (constant lead time).
     */
    public SafetyStockResult calculate(double zScore, double avgDemand, double demandStdDev, double leadTime) {
        return calculate(zScore, avgDemand, demandStdDev, leadTime, 0, 0);
    }

    /**
     * Get Z-score from service level using inverse normal approximation.
     */
    public static double zScoreFromServiceLevel(double serviceLevel) {
        if (serviceLevel <= 0) return -3.0;
        if (serviceLevel >= 1) return 3.0;

        // Rational approximation of the inverse normal CDF (Abramowitz & Stegun)
        double p = serviceLevel;
        if (p < 0.5) {
            double t = Math.sqrt(-2 * Math.log(p));
            return -(2.515517 + 0.802853 * t + 0.010328 * t * t) /
                    (1 + 1.432788 * t + 0.189269 * t * t + 0.001308 * t * t * t);
        } else {
            double t = Math.sqrt(-2 * Math.log(1 - p));
            return (2.515517 + 0.802853 * t + 0.010328 * t * t) /
                   (1 + 1.432788 * t + 0.189269 * t * t + 0.001308 * t * t * t);
        }
    }

    /**
     * Get service level description from Z-score.
     */
    public static String serviceLevelDescription(double zScore) {
        double sl = normalCDF(zScore);
        if (sl >= 0.99) return "Very high (critical items)";
        if (sl >= 0.97) return "High (important items)";
        if (sl >= 0.95) return "Standard (normal items)";
        if (sl >= 0.90) return "Moderate (low-priority items)";
        return "Low (non-critical items)";
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

    private SafetyStockResult zeroResult(double zScore, double avgD, double dStd, double lt, double ltStd, String explanation) {
        return new SafetyStockResult(0, zScore, dStd, lt, ltStd, 0, 0, 0, "SS = 0", explanation);
    }
}
