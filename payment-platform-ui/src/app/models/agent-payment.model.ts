export interface AgentPaymentSummary {
  userId: string;
  username: string;
  paymentCount: number;
  totalAmount: number;
  confirmedTotal: number;
  currency: string;
  payments: Payment[];
}

export interface Payment {
  id: string;
  reference: string;
  shopId: string;
  shopName: string;
  supplierId: string;
  supplierName: string;
  amount: number;
  currency: string;
  status: string;
  rejectionReason: string;
  createdBy: string;
  createdByName: string;
  confirmedByName: string;
  rejectedByName: string;
  cancelledByName: string;
  version: number;
  createdAt: string;
  updatedAt: string;
  events: any[];
}
