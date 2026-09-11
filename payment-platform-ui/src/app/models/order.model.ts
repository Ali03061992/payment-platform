export interface Order {
  id: string;
  reference: string;
  supplierId: string;
  shopId: string;
  supplierName: string | null;
  shopName: string | null;
  createdBy: string;
  createdByRole: string;
  source: string;
  status: string;
  subtotal: number;
  taxRate: number;
  taxAmount: number;
  total: number;
  currency: string;
  deliveryAgentId: string | null;
  receivedBy: string | null;
  receivedAt: string | null;
  deliveredAt: string | null;
  plannedDeliveryDate: string | null;
  confirmedDeliveryDate: string | null;
  asapPayment: boolean;
  notes: string | null;
  version: number;
  createdAt: string;
  updatedAt: string;
  items: OrderItem[];
  events: OrderEvent[];
}

export interface OrderItem {
  id: string;
  orderId: string;
  productId: string;
  productRef: string;
  productName: string;
  quantity: number;
  unitPrice: number;
  discount: number;
  lineTotal: number;
  createdAt: string;
}

export interface OrderEvent {
  id: string;
  orderId: string;
  action: string;
  userId: string | null;
  timestamp: string;
  details: string | null;
}

export interface CreateOrderRequest {
  supplierId: string;
  shopId: string;
  asapPayment: boolean;
  currency: string;
  notes: string;
  items: { productId: string; quantity: number; discount: number }[];
}
