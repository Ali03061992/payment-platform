export interface StockOptimizationResponse {
  products: ProductOptimization[];
  abcDistribution: { [key: string]: number };
  xyzDistribution: { [key: string]: number };
  abcxzDistribution: { [key: string]: number };
  actionDistribution: { [key: string]: number };
  riskDistribution: { [key: string]: number };
  totalStockValue: number;
  totalSafetyStockValue: number;
  productsToOrder: number;
  productsAtRisk: number;
  productsOverstocked: number;
  summary: string;
  calculatedAt: string;
}

export interface ProductOptimization {
  productId: number;
  productName: string;
  sku: string;
  abcClass: string;
  annualValue: number;
  abcRank: number;
  xyzClass: string;
  coefficientOfVariation: number;
  demandPattern: string;
  combinedClass: string;
  policyDescription: string;
  currentStock: number;
  reservedQty: number;
  availableStock: number;
  minQuantity: number;
  forecastMethod: string;
  avgDailyDemand: number;
  forecastAccuracy: number;
  forecastValues: number[];
  safetyStock: number;
  zScore: number;
  targetServiceLevel: number;
  safetyStockFormula: string;
  reorderPoint: number;
  shouldReorder: boolean;
  daysUntilReorder: number;
  economicOrderQty: number;
  adjustedOrderQty: number;
  ordersPerYear: number;
  totalAnnualCost: number;
  stockoutRisk: number;
  overstockRisk: number;
  daysOfSupply: number;
  turnoverRate: number;
  stockoutLevel: string;
  anomalies: AnomalyInfo[];
  healthScore: number;
  recommendation: RecommendationInfo;
  explanation: string;
  calculatedAt: string;
}

export interface AnomalyInfo {
  type: string;
  severity: string;
  description: string;
  detectedValue: number;
  expectedRange: number;
  explanation: string;
}

export interface RecommendationInfo {
  action: string;
  priority: string;
  orderQty: number;
  reorderPoint: number;
  safetyStock: number;
  targetServiceLevel: number;
  abcClass: string;
  xyzClass: string;
  combinedClass: string;
  stockoutRisk: number;
  overstockRisk: number;
  forecastMethod: string;
  reasoning: string;
  keyFactors: string[];
}
