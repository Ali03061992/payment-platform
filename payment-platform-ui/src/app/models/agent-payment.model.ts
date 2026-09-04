export interface AgentPaymentSummary {
  userId: number;
  username: string;
  paymentCount: number;
  totalAmount: number;
  confirmedTotal: number;
  currency: string;
  payments: Payment[];
}

export interface Payment {
  id: number;
  reference: string;
  shopId: number;
  shopName: string;
  supplierId: number;
  supplierName: string;
  amount: number;
  currency: string;
  status: string;
  rejectionReason: string;
  createdBy: number;
  createdByName: string;
  confirmedByName: string;
  rejectedByName: string;
  cancelledByName: string;
  version: number;
  createdAt: string;
  updatedAt: string;
  events: any[];
}
