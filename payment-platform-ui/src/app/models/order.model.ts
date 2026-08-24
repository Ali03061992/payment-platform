export interface Order {
  id: number;
  reference: string;
  supplierId: number;
  shopId: number;
  createdBy: number;
  createdByRole: string;
  source: string;
  status: string;
  subtotal: number;
  taxRate: number;
  taxAmount: number;
  total: number;
  currency: string;
  deliveryAgentId: number | null;
  receivedBy: number | null;
  receivedAt: string | null;
  deliveredAt: string | null;
  asapPayment: boolean;
  notes: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
  events: OrderEvent[];
}

export interface OrderItem {
  id: number;
  orderId: number;
  productId: number;
  productRef: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  discount: number;
  lineTotal: number;
  createdAt: string;
}

export interface OrderEvent {
  id: number;
  orderId: number;
  action: string;
  userId: number | null;
  timestamp: string;
  details: string | null;
}

export interface CreateOrderRequest {
  supplierId: number;
  shopId: number;
  asapPayment: boolean;
  currency: string;
  notes: string;
  items: { productId: number; quantity: number; discount: number }[];
}
