export type ReportGranularity = 'DAY' | 'WEEK' | 'MONTH' | 'YEAR';

export interface ReportSummary {
  revenue: number;
  orderCount: number;
  averageBasket: number;
  openOrders: number;
  deliveredOrders: number;
}

export interface ReportSeriesPoint {
  bucket: string;
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

export interface SupplierFinancialReport {
  from: string;
  to: string;
  previousFrom: string;
  previousTo: string;
  granularity: ReportGranularity;
  summary: ReportSummary;
  previousSummary: ReportSummary;
  series: ReportSeriesPoint[];
  previousSeries: ReportSeriesPoint[];
  orderCountByStatus: Record<string, number>;
  topProducts: TopProduct[];
  paymentSummary: PaymentSummary;
}
