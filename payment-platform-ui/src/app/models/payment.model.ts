export interface Payment {
  id: string;
  reference: string;
  shopId: string;
  shopName: string;
  supplierId: string;
  supplierName: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED';
  rejectionReason: string;
  createdBy: string;
  createdByName: string;
  confirmedByName: string;
  rejectedByName: string;
  cancelledByName: string;
  version: number;
  createdAt: string;
  updatedAt: string;
  events: PaymentEvent[];
}

export interface PaymentEvent {
  action: string;
  userId: string;
  userName: string;
  timestamp: string;
  details: string;
}

export interface PaymentStats {
  total: number;
  pending: number;
  confirmed: number;
  rejected: number;
  cancelled: number;
}

export interface CreatePaymentRequest {
  shopId: string;
  supplierId: string;
  amount: number;
  currency: string;
}

export interface RejectPaymentRequest {
  rejectionReason: string;
}
