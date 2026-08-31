export interface AgentPaymentSummary {
  userId: number;
  username: string;
  paymentCount: number;
  totalAmount: number;
  confirmedToday: number;
  currency: string;
  payments: Payment[];
}

export interface Payment {
  id: number;
  reference: string;
  shopId: number;
  supplierId: number;
  amount: number;
  currency: string;
  status: string;
  rejectionReason: string;
  createdBy: number;
  version: number;
  createdAt: string;
  updatedAt: string;
  events: any[];
}
