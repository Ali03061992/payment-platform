export interface SupplierFinancialReport {
  monthlyRevenue: MonthlyRevenue[];
  orderCountByStatus: Record<string, number>;
  topProducts: TopProduct[];
  paymentSummary: PaymentSummary;
}

export interface MonthlyRevenue {
  month: string;
  orderCount: number;
  revenue: number;
}

export interface TopProduct {
  productName: string;
  totalQuantity: number;
  totalRevenue: number;
}

export interface PaymentSummary {
  confirmedTotal: number;
  confirmedCount: number;
  pendingTotal: number;
  pendingCount: number;
  rejectedTotal: number;
  rejectedCount: number;
}
