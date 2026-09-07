package com.paymentplatform.organization.domain.engine;

/**
 * Economic Order Quantity calculator.
 * EOQ = sqrt(2 * D * S / H)
 * With adjustments for MOQ, multiples, capacity constraints.
 */
public class EOQCalculator {

    public record EOQResult(
        double economicOrderQty,
        double adjustedOrderQty,
        double annualDemand,
        double orderingCost,
        double holdingCostPerUnit,
        double moq,
        double totalAnnualCost,
        double ordersPerYear,
        double avgCycleStock,
        String explanation
    ) {}

    /**
     * Calculate EOQ with constraints.
     *
     * @param annualDemand D - annual demand quantity
     * @param orderingCost S - cost per order
     * @param holdingCostPerUnit H - annual holding cost per unit
     * @param moq minimum order quantity (0 if none)
     * @param multiple order multiple (1 if none)
     * @param maxCapacity maximum order quantity (0 if none)
     */
    public EOQResult calculate(
            double annualDemand,
            double orderingCost,
            double holdingCostPerUnit,
            double moq,
            double multiple,
            double maxCapacity) {

        if (annualDemand <= 0 || holdingCostPerUnit <= 0) {
            return zeroResult(annualDemand, orderingCost, holdingCostPerUnit, moq,
                "Cannot calculate EOQ: demand or holding cost is zero");
        }

        // Basic EOQ
        double eoq = Math.sqrt((2 * annualDemand * orderingCost) / holdingCostPerUnit);

        // Apply constraints
        double adjusted = eoq;

        // Apply MOQ
        if (moq > 0 && adjusted < moq) adjusted = moq;

        // Apply multiples
        if (multiple > 1) {
            adjusted = Math.ceil(adjusted / multiple) * multiple;
        }

        // Apply capacity constraint
        if (maxCapacity > 0 && adjusted > maxCapacity) adjusted = maxCapacity;

        // Final MOQ check after capacity
        if (moq > 0 && adjusted < moq) adjusted = moq;

        double ordersPerYear = annualDemand / adjusted;
        double avgCycleStock = adjusted / 2;
        double orderingCostTotal = ordersPerYear * orderingCost;
        double holdingCostTotal = avgCycleStock * holdingCostPerUnit;
        double totalCost = orderingCostTotal + holdingCostTotal;

        String explanation = String.format(
            "EOQ = sqrt(2 * %.0f * %.2f / %.2f) = %.0f | Adjusted: %.0f " +
            "(MOQ=%.0f, Multiple=%.0f) | Orders/year: %.1f | Total cost: %.2f",
            annualDemand, orderingCost, holdingCostPerUnit, eoq, adjusted,
            moq, multiple, ordersPerYear, totalCost
        );

        return new EOQResult(
            eoq, adjusted, annualDemand, orderingCost, holdingCostPerUnit, moq,
            totalCost, ordersPerYear, avgCycleStock, explanation
        );
    }

    /**
     * Estimate holding cost as percentage of unit cost.
     * Typical range: 15-30% of unit cost per year.
     */
    public static double estimateHoldingCost(double unitCost, double holdingPercent) {
        return unitCost * holdingPercent;
    }

    /**
     * Estimate ordering cost (administrative + logistics).
     * Default assumptions for the platform.
     */
    public static double defaultOrderingCost() {
        return 50.0; // EUR per order (configurable)
    }

    private EOQResult zeroResult(double ad, double oc, double hc, double moq, String explanation) {
        return new EOQResult(0, moq, ad, oc, hc, moq, 0, 0, 0, explanation);
    }
}
