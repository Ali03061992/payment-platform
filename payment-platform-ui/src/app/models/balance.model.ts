export interface BalanceEntry {
  id: number;
  supplierId: number;
  shopId: number;
  type: string;
  amount: number;
  balanceAfter: number;
  orderId: number | null;
  paymentId: number | null;
  reference: string | null;
  reason: string | null;
  createdBy: number | null;
  createdAt: string;
}

export interface BalanceSummary {
  supplierId: number;
  shopId: number;
  supplierName: string;
  shopName: string;
  currentBalance: number;
  totalOrders: number;
  totalPayments: number;
  remainingDue: number;
  lastTransaction: string;
}
