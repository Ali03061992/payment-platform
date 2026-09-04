export interface Payment {
  id: number;
  reference: string;
  shopId: number;
  shopName: string;
  supplierId: number;
  supplierName: string;
  amount: number;
  currency: string;
  status: 'PENDING' | 'CONFIRMED' | 'REJECTED' | 'CANCELLED';
  rejectionReason: string;
  createdBy: number;
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
  userId: number;
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
  shopId: number;
  supplierId: number;
  amount: number;
  currency: string;
}

export interface RejectPaymentRequest {
  rejectionReason: string;
}
