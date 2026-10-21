export interface Dispute {
  id: string;
  orderId: string;
  shopId: string;
  supplierId: string;
  openedBy: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
  reason: string;
  messages: DisputeMessage[];
  version: number;
  createdAt: string;
  updatedAt: string;
}

export interface DisputeMessage {
  id: string;
  senderId: string;
  senderRole: 'SHOP' | 'SUPPLIER';
  content: string;
  timestamp: string;
}

export interface CreateDisputeRequest {
  orderId: string;
  reason: string;
}

export interface AddDisputeMessageRequest {
  content: string;
}
