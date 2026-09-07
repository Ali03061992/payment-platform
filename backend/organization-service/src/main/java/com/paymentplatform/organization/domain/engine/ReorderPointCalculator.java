package com.paymentplatform.organization.domain.engine;

/**
 * Dynamic reorder point calculator.
 * ROP = Expected demand during lead time + Safety stock - Outstanding orders
 */
public class ReorderPointCalculator {

    public record ReorderPointResult(
        double reorderPoint,
        double demandDuringLeadTime,
        double safetyStock,
        double outstandingOrders,
        double currentInventoryPosition,
        boolean shouldReorder,
        double daysUntilReorder,
        String explanation
    ) {}

    /**
     * Calculate reorder point.
     *
     * @param avgDailyDemand average demand per day
     * @param leadTimeDays lead time in days
     * @param safetyStock calculated safety stock
     * @param currentAvailable current available stock
     * @param reservedQty reserved quantity
     * @param transitQty stock in transit
     * @param outstandingOrders incoming orders not yet received
     */
    public ReorderPointResult calculate(
            double avgDailyDemand,
            double leadTimeDays,
            double safetyStock,
            double currentAvailable,
            double reservedQty,
            double transitQty,
            double outstandingOrders) {

        double demandDuringLT = avgDailyDemand * leadTimeDays;
        double rop = demandDuringLT + safetyStock;

        // Inventory position = available - reserved + in transit + outstanding orders
        double inventoryPosition = currentAvailable - reservedQty + transitQty + outstandingOrders;
        boolean shouldReorder = inventoryPosition <= rop;

        // Days until stock reaches ROP
        double currentExcess = inventoryPosition - rop;
        double daysUntilReorder = avgDailyDemand > 0 ? currentExcess / avgDailyDemand : Double.MAX_VALUE;

        if (daysUntilReorder < 0) daysUntilReorder = 0;

        String explanation = String.format(
            "ROP = Demand during LT (%.1f) + Safety stock (%.0f) = %.0f | " +
            "Current position: %.0f | Should reorder: %s | Days until reorder: %.1f",
            demandDuringLT, safetyStock, rop, inventoryPosition,
            shouldReorder ? "YES" : "No", daysUntilReorder
        );

        return new ReorderPointResult(
            rop, demandDuringLT, safetyStock, outstandingOrders,
            inventoryPosition, shouldReorder, daysUntilReorder, explanation
        );
    }

    /**
     * Simple ROP without outstanding orders consideration.
     */
    public ReorderPointResult calculateSimple(double avgDailyDemand, double leadTimeDays, double safetyStock) {
        return calculate(avgDailyDemand, leadTimeDays, safetyStock, 0, 0, 0, 0);
    }
}
