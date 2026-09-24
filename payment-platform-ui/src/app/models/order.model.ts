export interface Order {
  id: string;
  reference: string;
  supplierId: string;
  shopId: string;
  supplierName: string | null;
  shopName: string | null;
  createdBy: string;
  createdByName: string | null;
  createdByRole: string;
  source: string;
  status: string;
  subtotal: number;
  taxRate: number;
  taxAmount: number;
  globalDiscount: number;
  total: number;
  currency: string;
  deliveryAgentId: string | null;
  deliveryAgentName: string | null;
  receivedBy: string | null;
  receivedByName: string | null;
  receivedAt: string | null;
  deliveredAt: string | null;
  plannedDeliveryDate: string | null;
  confirmedDeliveryDate: string | null;
  asapPayment: boolean;
  paymentTerms: string | null;
  dueDate: string | null;
  deliveryRejectionReason: string | null;
  estimatedArrival: string | null;
  lastLatitude: number | null;
  lastLongitude: number | null;
  lastLocationUpdate: string | null;
  notes: string | null;
  confirmedByName: string | null;
  preparedByName: string | null;
  readyByName: string | null;
  assignedDeliveryByName: string | null;
  acceptedDeliveryByName: string | null;
  confirmedDeliveryByName: string | null;
  deliveredByName: string | null;
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
  paymentTerms: string;
  currency: string;
  globalDiscount: number;
  taxRate?: number;
  notes: string;
  items: { productId: string; quantity: number; discount: number }[];
}

export interface UpdateOrderRequest {
  notes: string | null;
  asapPayment: boolean;
  items: { productId: string; quantity: number; discount: number }[];
}

export interface OrderComment {
  id: string;
  orderId: string;
  authorId: string;
  authorName: string;
  content: string;
  createdAt: string;
}
