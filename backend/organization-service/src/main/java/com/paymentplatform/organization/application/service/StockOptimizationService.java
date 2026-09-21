package com.paymentplatform.organization.application.service;

import com.paymentplatform.organization.application.dto.StockOptimizationResponse;
import com.paymentplatform.organization.domain.engine.StockOptimizationEngine;
import com.paymentplatform.organization.domain.model.Product;
import com.paymentplatform.organization.domain.model.StockMovement;
import com.paymentplatform.organization.domain.repository.ProductRepository;
import com.paymentplatform.organization.domain.repository.StockMovementRepository;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class StockOptimizationService {

    private final ProductRepository productRepository;
    private final StockMovementRepository movementRepository;
    private final StockOptimizationEngine engine = new StockOptimizationEngine();

    // Configurable parameters
    private double defaultLeadTimeDays = 7;
    private double orderingCost = 50.0;
    private double holdingCostPercent = 0.25;

    public StockOptimizationService(ProductRepository productRepository, StockMovementRepository movementRepository) {
        this.productRepository = productRepository;
        this.movementRepository = movementRepository;
    }

    public StockOptimizationResponse optimize(UUID supplierId) {
        List<Product> products = productRepository.findBySupplierId(supplierId);

        // Load movements for each product
        Map<UUID, List<StockMovement>> movementsByProduct = new HashMap<>();
        Map<UUID, double[]> demandByProduct = new HashMap<>();

        for (Product p : products) {
            List<StockMovement> movements = movementRepository.findByProductIdOrderByCreatedAtDesc(p.getId());
            movementsByProduct.put(p.getId(), movements);

            // Derive demand history from OUT movements
            double[] demand = deriveDemandFromMovements(movements);
            if (demand.length > 0) {
                demandByProduct.put(p.getId(), demand);
            }
        }

        StockOptimizationEngine.OptimizationResult result = engine.optimize(
            products, movementsByProduct, demandByProduct,
            defaultLeadTimeDays, orderingCost, holdingCostPercent
        );

        // Convert to DTO
        List<StockOptimizationResponse.ProductOptimizationResult> productResults = new ArrayList<>();
        for (StockOptimizationEngine.ProductOptimization r : result.products()) {
            List<StockOptimizationResponse.AnomalyInfo> anomalies = r.anomalies().stream()
                .map(StockOptimizationResponse.AnomalyInfo::from).toList();
            StockOptimizationResponse.RecommendationInfo rec = StockOptimizationResponse.RecommendationInfo.from(r.recommendation());

            productResults.add(new StockOptimizationResponse.ProductOptimizationResult(
                r.productId(), r.productName(), r.sku(),
                r.abcClass(), r.annualValue(), r.abcRank(),
                r.xyzClass(), r.coefficientOfVariation(), r.demandPattern(),
                r.combinedClass(), r.policyDescription(),
                r.currentStock(), r.reservedQty(), r.availableStock(), r.minQuantity(),
                r.forecastMethod(), r.avgDailyDemand(), r.forecastAccuracy(), r.forecastValues(),
                r.safetyStock(), r.zScore(), r.targetServiceLevel(), r.safetyStockFormula(),
                r.reorderPoint(), r.shouldReorder(), r.daysUntilReorder(),
                r.economicOrderQty(), r.adjustedOrderQty(), r.ordersPerYear(), r.totalAnnualCost(),
                r.stockoutRisk(), r.overstockRisk(), r.daysOfSupply(), r.turnoverRate(), r.stockoutLevel(),
                anomalies, r.healthScore(), rec, r.explanation(), r.calculatedAt()
            ));
        }

        return new StockOptimizationResponse(
            productResults, result.abcDistribution(), result.xyzDistribution(),
            result.abcxzDistribution(), result.actionDistribution(), result.riskDistribution(),
            result.totalStockValue(), result.totalSafetyStockValue(),
            result.productsToOrder(), result.productsAtRisk(), result.productsOverstocked(),
            result.summary(), result.calculatedAt()
        );
    }

    public void setParameters(double leadTimeDays, double orderingCost, double holdingCostPercent) {
        this.defaultLeadTimeDays = leadTimeDays;
        this.orderingCost = orderingCost;
        this.holdingCostPercent = holdingCostPercent;
    }

    private double[] deriveDemandFromMovements(List<StockMovement> movements) {
        if (movements == null || movements.isEmpty()) return new double[0];

        Map<String, Double> dailyDemand = new TreeMap<>();
        for (StockMovement m : movements) {
            if ("OUT".equals(m.getType()) && m.getCreatedAt() != null) {
                String day = m.getCreatedAt().toString().substring(0, 10);
                dailyDemand.merge(day, (double) m.getQuantity(), Double::sum);
            }
        }

        if (dailyDemand.isEmpty()) return new double[0];
        return dailyDemand.values().stream().mapToDouble(Double::doubleValue).toArray();
    }
}
