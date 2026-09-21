package com.paymentplatform.organization.domain.engine;

import java.util.Arrays;

/**
 * XYZ Classification based on demand variability (coefficient of variation).
 * X: stable, Y: variable, Z: sporadic/intermittent.
 */
public class XYZClassifier {

    public record XYZResult(
        String classification,
        double coefficientOfVariation,
        double meanDemand,
        double stdDev,
        String demandPattern,
        String explanation
    ) {
        public boolean isX() { return "X".equals(classification); }
        public boolean isY() { return "Y".equals(classification); }
        public boolean isZ() { return "Z".equals(classification); }
    }

    private final double thresholdX;
    private final double thresholdY;

    /**
     * @param thresholdX CV below which = X (default 0.5)
     * @param thresholdY CV below which = Y (default 1.0)
     */
    public XYZClassifier(double thresholdX, double thresholdY) {
        this.thresholdX = thresholdX;
        this.thresholdY = thresholdY;
    }

    public XYZClassifier() {
        this(0.5, 1.0);
    }

    /**
     * Classify a product's demand pattern.
     * @param demandHistory historical demand data
     */
    public XYZResult classify(double[] demandHistory) {
        if (demandHistory == null || demandHistory.length < 3) {
            return new XYZResult("Z", 0, 0, 0, "Insufficient data",
                "Cannot classify: need at least 3 demand periods");
        }

        double mean = Arrays.stream(demandHistory).average().orElse(0);
        double variance = 0;
        for (double v : demandHistory) variance += (v - mean) * (v - mean);
        variance /= (demandHistory.length - 1);
        double stdDev = Math.sqrt(variance);

        double cv = mean > 0 ? stdDev / mean : Double.MAX_VALUE;

        // Detect intermittent demand
        long zeros = Arrays.stream(demandHistory).filter(v -> v <= 0).count();
        double zeroRatio = (double) zeros / demandHistory.length;
        boolean intermittent = zeroRatio > 0.5;

        String classif;
        String pattern;

        if (intermittent) {
            classif = "Z";
            pattern = "Intermittent";
        } else if (cv < thresholdX) {
            classif = "X";
            pattern = "Stable";
        } else if (cv < thresholdY) {
            classif = "Y";
            pattern = "Variable";
        } else {
            classif = "Z";
            pattern = cv > 2.0 ? "Highly sporadic" : "Sporadic";
        }

        String explanation = String.format(
            "Mean demand: %.2f | StdDev: %.2f | CV: %.3f | Zero periods: %d/%d (%.0f%%) | Pattern: %s | Class: %s",
            mean, stdDev, cv, zeros, demandHistory.length, zeroRatio * 100, pattern, classif
        );

        return new XYZResult(classif, cv, mean, stdDev, pattern, explanation);
    }

    /**
     * Get review method based on XYZ class.
     */
    public String reviewMethod(String xyzClass) {
        return switch (xyzClass) {
            case "X" -> "Continuous review (ROP)";
            case "Y" -> "Periodic review (s,S)";
            case "Z" -> "Order-up-to / Min-Max";
            default -> "Periodic review";
        };
    }

    /**
     * Get demand forecasting suitability.
     */
    public String forecastingNote(String xyzClass) {
        return switch (xyzClass) {
            case "X" -> "Classical forecasting methods applicable (SES, Holt, Holt-Winters)";
            case "Y" -> "Methods with higher smoothing parameter recommended";
            case "Z" -> "Croston or SBA method recommended for intermittent demand";
            default -> "Standard methods";
        };
    }

    /**
     * Combine ABC and XYZ into a policy matrix.
     */
    public static String combinedPolicy(String abc, String xyz) {
        return abc + xyz;
    }

    /**
     * Get recommended policy for combined ABC-XYZ class.
     */
    public static String policyDescription(String abcxz) {
        return switch (abcxz) {
            case "AX" -> "High value, stable demand. Continuous review, high service level (98-99%), low safety stock relative to demand.";
            case "AY" -> "High value, variable demand. Continuous review, high service level, higher safety stock, frequent forecasting.";
            case "AZ" -> "High value, sporadic demand. Careful monitoring, Croston forecasting, safety stock per demand pattern.";
            case "BX" -> "Medium value, stable demand. Periodic review, moderate service level (95%), standard safety stock.";
            case "BY" -> "Medium value, variable demand. Periodic review, moderate safety stock, re-evaluate demand pattern regularly.";
            case "BZ" -> "Medium value, sporadic demand. Min-max policy, moderate service level, monitor for obsolescence.";
            case "CX" -> "Low value, stable demand. Simple reorder, lower service level (90%), bulk ordering OK.";
            case "CY" -> "Low value, variable demand. Periodic review, lower safety stock, consider vendor-managed inventory.";
            case "CZ" -> "Low value, sporadic demand. Consider drop-shipping, on-demand ordering, or discontinuation.";
            default -> "Standard inventory management.";
        };
    }
}
