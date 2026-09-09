export interface BalanceEntry {
  id: string;
  supplierId: string;
  shopId: string;
  type: string;
  amount: number;
  balanceAfter: number;
  orderId: string | null;
  paymentId: string | null;
  reference: string | null;
  reason: string | null;
  createdBy: string | null;
  createdAt: string;
}

export interface BalanceSummary {
  supplierId: string;
  shopId: string;
  supplierName: string;
  shopName: string;
  currentBalance: number;
  totalOrders: number;
  totalPayments: number;
  remainingDue: number;
  lastTransaction: string;
}
