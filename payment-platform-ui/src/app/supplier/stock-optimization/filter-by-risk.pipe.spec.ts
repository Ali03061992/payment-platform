// @ts-nocheck
import { FilterByRiskPipe } from './filter-by-risk.pipe';
import { ProductOptimization } from '../../models/stock-optimization.model';

describe('FilterByRiskPipe', () => {
  let pipe: FilterByRiskPipe;

  beforeEach(() => {
    pipe = new FilterByRiskPipe();
  });

  function createProduct(overrides: Partial<ProductOptimization> = {}): ProductOptimization {
    return {
      productId: 1, productName: 'Test', sku: 'SKU-1', abcClass: 'A', annualValue: 1000,
      abcRank: 1, xyzClass: 'X', coefficientOfVariation: 0.1, demandPattern: 'stable',
      combinedClass: 'AX', policyDescription: '', currentStock: 50, reservedQty: 0,
      availableStock: 50, minQuantity: 10, forecastMethod: 'avg', avgDailyDemand: 5,
      forecastAccuracy: 0.9, forecastValues: [], safetyStock: 10, zScore: 1.65,
      targetServiceLevel: 0.95, safetyStockFormula: '', reorderPoint: 45,
      shouldReorder: false, daysUntilReorder: 10, economicOrderQty: 100,
      adjustedOrderQty: 100, ordersPerYear: 12, totalAnnualCost: 5000,
      stockoutRisk: 0.1, overstockRisk: 0.2, daysOfSupply: 10, turnoverRate: 12,
      stockoutLevel: 'LOW', anomalies: [], healthScore: 80,
      recommendation: { action: 'NO_ACTION', priority: 'LOW', orderQty: 0, reorderPoint: 45,
        safetyStock: 10, targetServiceLevel: 0.95, abcClass: 'A', xyzClass: 'X',
        combinedClass: 'AX', stockoutRisk: 0.1, overstockRisk: 0.2, forecastMethod: 'avg',
        reasoning: '', keyFactors: [] },
      explanation: '', calculatedAt: '', ...overrides
    };
  }

  it('should create an instance', () => {
    expect(pipe).toBeTruthy();
  });

  it('should return empty array for null input', () => {
    expect(pipe.transform(null as any)).toEqual([]);
  });

  it('should return empty array for undefined input', () => {
    expect(pipe.transform(undefined as any)).toEqual([]);
  });

  it('should filter HIGH stockout level products', () => {
    const products = [
      createProduct({ productId: 1, stockoutLevel: 'HIGH' }),
      createProduct({ productId: 2, stockoutLevel: 'LOW' })
    ];
    const result = pipe.transform(products);
    expect(result.length).toBe(1);
    expect(result[0].productId).toBe(1);
  });

  it('should filter CRITICAL stockout level products', () => {
    const products = [
      createProduct({ productId: 1, stockoutLevel: 'CRITICAL' }),
      createProduct({ productId: 2, stockoutLevel: 'LOW' })
    ];
    const result = pipe.transform(products);
    expect(result.length).toBe(1);
    expect(result[0].stockoutLevel).toBe('CRITICAL');
  });

  it('should filter products with overstockRisk > 0.5', () => {
    const products = [
      createProduct({ productId: 1, overstockRisk: 0.8 }),
      createProduct({ productId: 2, overstockRisk: 0.3 })
    ];
    const result = pipe.transform(products);
    expect(result.length).toBe(1);
    expect(result[0].overstockRisk).toBe(0.8);
  });

  it('should filter products with ORDER_NOW action', () => {
    const products = [
      createProduct({ productId: 1, recommendation: { action: 'ORDER_NOW' } as any }),
      createProduct({ productId: 2, recommendation: { action: 'MONITOR' } as any })
    ];
    const result = pipe.transform(products);
    expect(result.length).toBe(1);
    expect(result[0].recommendation.action).toBe('ORDER_NOW');
  });

  it('should return multiple matching products', () => {
    const products = [
      createProduct({ productId: 1, stockoutLevel: 'CRITICAL' }),
      createProduct({ productId: 2, overstockRisk: 0.7 }),
      createProduct({ productId: 3, recommendation: { action: 'ORDER_NOW' } as any }),
      createProduct({ productId: 4, stockoutLevel: 'LOW', overstockRisk: 0.1, recommendation: { action: 'MONITOR' } as any })
    ];
    const result = pipe.transform(products);
    expect(result.length).toBe(3);
  });

  it('should return empty array when no products match', () => {
    const products = [
      createProduct({ stockoutLevel: 'LOW', overstockRisk: 0.1, recommendation: { action: 'MONITOR' } as any }),
      createProduct({ stockoutLevel: 'OK', overstockRisk: 0.2, recommendation: { action: 'NO_ACTION' } as any })
    ];
    const result = pipe.transform(products);
    expect(result.length).toBe(0);
  });
});
