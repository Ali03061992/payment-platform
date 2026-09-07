package com.paymentplatform.organization.domain.engine;

import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.model.StockMovement;

import java.util.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Main stock optimization engine.
 * Orchestrates all analysis components and produces comprehensive results.
 */
public class StockOptimizationEngine {

    private final DemandForecaster forecaster = new DemandForecaster();
    private final ABCClassifier abcClassifier = new ABCClassifier();
    private final XYZClassifier xyzClassifier = new XYZClassifier();
    private final SafetyStockCalculator safetyCalc = new SafetyStockCalculator();
    private final ReorderPointCalculator ropCalc = new ReorderPointCalculator();
    private final EOQCalculator eoqCalc = new EOQCalculator();
    private final RiskAnalyzer riskAnalyzer = new RiskAnalyzer();
    private final AnomalyDetector anomalyDetector = new AnomalyDetector();
    private final RecommendationEngine recEngine = new RecommendationEngine();

    public record ProductOptimization(
        Long productId,
        String productName,
        String sku,
        // Classification
        String abcClass,
        double annualValue,
        int abcRank,
        String xyzClass,
        double coefficientOfVariation,
        String demandPattern,
        String combinedClass,
        String policyDescription,
        // Current stock
        double currentStock,
        double reservedQty,
        double availableStock,
        double minQuantity,
        // Forecast
        String forecastMethod,
        double avgDailyDemand,
        double forecastAccuracy,
        double[] forecastValues,
        // Safety stock
        double safetyStock,
        double zScore,
        double targetServiceLevel,
        String safetyStockFormula,
        // Reorder point
        double reorderPoint,
        boolean shouldReorder,
        double daysUntilReorder,
        // EOQ
        double economicOrderQty,
        double adjustedOrderQty,
        double ordersPerYear,
        double totalAnnualCost,
        // Risk
        double stockoutRisk,
        double overstockRisk,
        double daysOfSupply,
        double turnoverRate,
        String stockoutLevel,
        // Anomalies
        List<AnomalyDetector.Anomaly> anomalies,
        double healthScore,
        // Recommendation
        RecommendationEngine.Recommendation recommendation,
        // Metadata
        String explanation,
        Instant calculatedAt
    ) {}

    public record OptimizationResult(
        List<ProductOptimization> products,
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
    ) {}

    /**
     * Run full optimization for all products of a supplier.
     */
    public OptimizationResult optimize(
            List<Product> products,
            Map<Long, List<StockMovement>> movementsByProduct,
            Map<Long, double[]> demandHistoryByProduct,
            double defaultLeadTimeDays,
            double orderingCost,
            double holdingCostPercent) {

        List<ProductOptimization> results = new ArrayList<>();

        for (Product product : products) {
            try {
                ProductOptimization opt = optimizeProduct(
                    product, movementsByProduct.getOrDefault(product.getId(), List.of()),
                    demandHistoryByProduct.get(product.getId()),
                    defaultLeadTimeDays, orderingCost, holdingCostPercent
                );
                results.add(opt);
            } catch (Exception e) {
                // Skip products with errors
            }
        }

        // Compute distributions
        Map<String, Long> abcDist = new LinkedHashMap<>();
        Map<String, Long> xyzDist = new LinkedHashMap<>();
        Map<String, Long> abcxzDist = new LinkedHashMap<>();
        Map<String, Long> actionDist = new LinkedHashMap<>();
        Map<String, Long> riskDist = new LinkedHashMap<>();

        for (ProductOptimization r : results) {
            abcDist.merge(r.abcClass(), 1L, Long::sum);
            xyzDist.merge(r.xyzClass(), 1L, Long::sum);
            abcxzDist.merge(r.combinedClass(), 1L, Long::sum);
            actionDist.merge(r.recommendation().action().name(), 1L, Long::sum);
            riskDist.merge(r.stockoutLevel(), 1L, Long::sum);
        }

        double totalValue = results.stream().mapToDouble(r -> r.currentStock() * getUnitCost(products, r.productId())).sum();
        double totalSSValue = results.stream().mapToDouble(r -> r.safetyStock() * getUnitCost(products, r.productId())).sum();
        int toOrder = (int) results.stream().filter(r -> r.recommendation().action() == RecommendationEngine.Action.ORDER_NOW ||
            r.recommendation().action() == RecommendationEngine.Action.ORDER_SOON).count();
        int atRisk = (int) results.stream().filter(r -> "HIGH".equals(r.stockoutLevel()) || "CRITICAL".equals(r.stockoutLevel())).count();
        int overstocked = (int) results.stream().filter(r -> r.overstockRisk() > 0.7).count();

        String summary = recEngine.generateSummary(results.stream()
            .map(ProductOptimization::recommendation).toList());

        return new OptimizationResult(
            results, abcDist, xyzDist, abcxzDist, actionDist, riskDist,
            totalValue, totalSSValue, toOrder, atRisk, overstocked,
            summary, Instant.now()
        );
    }

    private ProductOptimization optimizeProduct(
            Product product,
            List<StockMovement> movements,
            double[] demandHistory,
            double defaultLeadTimeDays,
            double orderingCost,
            double holdingCostPercent) {

        long pid = product.getId();
        String name = product.getName();
        String sku = product.getSku();
        double currentStock = product.getQuantity();
        double reservedQty = product.getReservedQty() != null ? product.getReservedQty() : 0;
        double availableStock = currentStock - reservedQty;
        double minQuantity = product.getMinQuantity();
        double unitCost = product.getUnitPrice() != null ? product.getUnitPrice().doubleValue() : 0;

        // Prepare demand history
        double[] demand = prepareDemandData(demandHistory, movements, product);

        // ABC classification (use annual demand * unit cost)
        double annualDemand = Arrays.stream(demand).sum() * (demand.length > 0 ? 365.0 / demand.length : 0);
        Map<Long, ABCClassifier.ABCResult> abcResults = abcClassifier.classify(
            List.of(pid), new double[]{annualDemand}, new double[]{unitCost}
        );
        ABCClassifier.ABCResult abcResult = abcResults.get(pid);
        String abcClass = abcResult != null ? abcResult.classification() : "C";
        double annualValue = abcResult != null ? abcResult.annualValue() : 0;
        int abcRank = abcResult != null ? abcResult.rank() : 0;

        // XYZ classification
        XYZClassifier.XYZResult xyzResult = xyzClassifier.classify(demand);
        String xyzClass = xyzResult.classification();
        double cv = xyzResult.coefficientOfVariation();

        // Forecast
        DemandForecaster.ForecastResult forecast = forecaster.forecast(demand, 30);
        double avgDailyDemand = forecast.forecasts().length > 0 ?
            Arrays.stream(forecast.forecasts()).average().orElse(0) : xyzResult.meanDemand();

        // Compute demand std dev
        double demandStdDev = xyzResult.stdDev();
        if (demandStdDev == 0 && demand.length > 1) {
            double mean = Arrays.stream(demand).average().orElse(0);
            double variance = 0;
            for (double d : demand) variance += (d - mean) * (d - mean);
            demandStdDev = Math.sqrt(variance / (demand.length - 1));
        }

        // Lead time (default or from data)
        double leadTimeDays = defaultLeadTimeDays;
        double leadTimeStdDev = leadTimeDays * 0.2; // Assume 20% variability if no data

        // Target service level
        double serviceLevel = abcClassifier.targetServiceLevel(abcClass);
        double zScore = SafetyStockCalculator.zScoreFromServiceLevel(serviceLevel);

        // Safety stock
        SafetyStockCalculator.SafetyStockResult ssResult = safetyCalc.calculate(
            zScore, avgDailyDemand, demandStdDev, leadTimeDays, leadTimeStdDev, 0
        );
        double safetyStock = ssResult.safetyStock();

        // Reorder point
        double outstandingOrders = 0;
        ReorderPointCalculator.ReorderPointResult ropResult = ropCalc.calculate(
            avgDailyDemand, leadTimeDays, safetyStock,
            currentStock, reservedQty, 0, outstandingOrders
        );

        // EOQ
        double annualDemandForEOQ = avgDailyDemand * 365;
        double holdingCost = EOQCalculator.estimateHoldingCost(unitCost, holdingCostPercent);
        EOQCalculator.EOQResult eoqResult = eoqCalc.calculate(
            annualDemandForEOQ, orderingCost, holdingCost,
            minQuantity, 1, 0
        );

        // Risk
        RiskAnalyzer.RiskResult riskResult = riskAnalyzer.analyze(
            currentStock, reservedQty, avgDailyDemand, demandStdDev,
            leadTimeDays, leadTimeStdDev, safetyStock, unitCost, annualDemandForEOQ
        );

        // Anomalies
        double[] recentDemand = demand.length > 30 ?
            Arrays.copyOfRange(demand, demand.length - 30, demand.length) : demand;
        List<AnomalyDetector.Anomaly> anomalies = anomalyDetector.detect(
            currentStock, reservedQty, minQuantity,
            avgDailyDemand, demandStdDev, leadTimeDays,
            recentDemand, demand
        );
        double healthScore = anomalyDetector.healthScore(anomalies);

        // Recommendation
        RecommendationEngine.Recommendation recommendation = recEngine.generate(
            pid, name, currentStock, reservedQty, minQuantity,
            abcClass, xyzClass, forecast.method(),
            avgDailyDemand, safetyStock, zScore,
            ropResult.reorderPoint(), ropResult.shouldReorder(),
            eoqResult.adjustedOrderQty(),
            riskResult.stockoutRisk(), riskResult.overstockRisk(), riskResult.daysOfSupply(),
            anomalies
        );

        // Build explanation
        String explanation = String.format(
            "Product: %s (%s) | Class: %s%s | Forecast: %s (accuracy: %.1f%%) | " +
            "Safety stock: %.0f | ROP: %.0f | EOQ: %.0f | Risk: %s",
            name, sku, abcClass, xyzClass, forecast.method(), forecast.accuracy(),
            safetyStock, ropResult.reorderPoint(), eoqResult.adjustedOrderQty(),
            riskResult.stockoutLevel()
        );

        return new ProductOptimization(
            pid, name, sku,
            abcClass, annualValue, abcRank,
            xyzClass, cv, xyzResult.demandPattern(), abcClass + xyzClass,
            XYZClassifier.policyDescription(abcClass + xyzClass),
            currentStock, reservedQty, availableStock, minQuantity,
            forecast.method(), avgDailyDemand, forecast.accuracy(), forecast.forecasts(),
            safetyStock, zScore, serviceLevel, ssResult.formula(),
            ropResult.reorderPoint(), ropResult.shouldReorder(), ropResult.daysUntilReorder(),
            eoqResult.economicOrderQty(), eoqResult.adjustedOrderQty(),
            eoqResult.ordersPerYear(), eoqResult.totalAnnualCost(),
            riskResult.stockoutRisk(), riskResult.overstockRisk(),
            riskResult.daysOfSupply(), riskResult.turnoverRate(), riskResult.stockoutLevel(),
            anomalies, healthScore, recommendation,
            explanation, Instant.now()
        );
    }

    private double[] prepareDemandData(double[] demandHistory, List<StockMovement> movements, Product product) {
        if (demandHistory != null && demandHistory.length > 0) {
            return demandHistory;
        }

        // Derive demand from movements (OUT movements)
        if (movements != null && !movements.isEmpty()) {
            // Group by day
            Map<String, Double> dailyDemand = new TreeMap<>();
            for (StockMovement m : movements) {
                if ("OUT".equals(m.getType()) && m.getCreatedAt() != null) {
                    String day = m.getCreatedAt().toString().substring(0, 10);
                    dailyDemand.merge(day, (double) m.getQuantity(), Double::sum);
                }
            }

            if (!dailyDemand.isEmpty()) {
                return dailyDemand.values().stream().mapToDouble(Double::doubleValue).toArray();
            }
        }

        // If we have current quantity and minQuantity, estimate
        if (product.getQuantity() > 0 || product.getMinQuantity() > 0) {
            double estimated = Math.max(product.getMinQuantity() * 0.5, 1);
            return new double[]{estimated};
        }

        return new double[0];
    }

    private double getUnitCost(List<Product> products, Long productId) {
        return products.stream()
            .filter(p -> p.getId().equals(productId))
            .findFirst()
            .map(p -> p.getUnitPrice() != null ? p.getUnitPrice().doubleValue() : 0)
            .orElse(0.0);
    }
}
