package com.paymentplatform.organization.domain.engine;

import java.util.UUID;

import java.util.*;

/**
 * ABC Classification based on annual consumption value (Pareto principle).
 * Configurable thresholds.
 */
public class ABCClassifier {

    public record ABCResult(
        String classification,
        double annualValue,
        double cumulativePercent,
        int rank,
        String explanation
    ) {
        public boolean isA() { return "A".equals(classification); }
        public boolean isB() { return "B".equals(classification); }
        public boolean isC() { return "C".equals(classification); }
    }

    private final double thresholdA;
    private final double thresholdB;

    /**
     * @param thresholdA cumulative % below which = A (default 0.80 = 80%)
     * @param thresholdB cumulative % below which = B (default 0.95 = 95%)
     */
    public ABCClassifier(double thresholdA, double thresholdB) {
        this.thresholdA = thresholdA;
        this.thresholdB = thresholdB;
    }

    public ABCClassifier() {
        this(0.80, 0.95);
    }

    /**
     * Classify a list of products.
     * @param productIds product identifiers
     * @param annualDemand annual consumption quantities
     * @param unitCosts unit costs
     * @return map productId -> ABCResult
     */
    public Map<UUID, ABCResult> classify(List<UUID> productIds, double[] annualDemand, double[] unitCosts) {
        int n = productIds.size();
        double[] annualValues = new double[n];
        for (int i = 0; i < n; i++) {
            annualValues[i] = annualDemand[i] * unitCosts[i];
        }

        // Sort indices by annual value descending
        Integer[] sortedIndices = new Integer[n];
        for (int i = 0; i < n; i++) sortedIndices[i] = i;
        Arrays.sort(sortedIndices, (a, b) -> Double.compare(annualValues[b], annualValues[a]));

        double totalValue = Arrays.stream(annualValues).sum();
        if (totalValue == 0) totalValue = 1;

        Map<UUID, ABCResult> results = new LinkedHashMap<>();
        double cumulative = 0;

        for (int rank = 0; rank < n; rank++) {
            int idx = sortedIndices[rank];
            cumulative += annualValues[idx];
            double cumPercent = cumulative / totalValue;

            String classif;
            if (cumPercent <= thresholdA) classif = "A";
            else if (cumPercent <= thresholdB) classif = "B";
            else classif = "C";

            String explanation = String.format(
                "Rank #%d | Annual value: %.2f | Cumulative: %.1f%% | Class: %s",
                rank + 1, annualValues[idx], cumPercent * 100, classif
            );

            results.put(productIds.get(idx), new ABCResult(
                classif, annualValues[idx], cumPercent, rank + 1, explanation
            ));
        }

        return results;
    }

    /**
     * Get target service level based on ABC class.
     * A items: 98-99%, B items: 95%, C items: 90%
     */
    public double targetServiceLevel(String abcClass) {
        return switch (abcClass) {
            case "A" -> 0.98;
            case "B" -> 0.95;
            case "C" -> 0.90;
            default -> 0.95;
        };
    }

    /**
     * Get suggested review frequency based on ABC class.
     */
    public String reviewFrequency(String abcClass) {
        return switch (abcClass) {
            case "A" -> "Weekly";
            case "B" -> "Bi-weekly";
            case "C" -> "Monthly";
            default -> "Monthly";
        };
    }
}
