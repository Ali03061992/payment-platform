export type DisputePriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT';

export interface Dispute {
  id: string;
  orderId: string;
  shopId: string;
  supplierId: string;
  openedBy: string;
  status: 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
  reason: string;
  priority: DisputePriority;
  dueAt?: string | null;
  assignedTo?: string | null;
  messages: DisputeMessage[];
  history: DisputeStatusHistory[];
  attachments: DisputeAttachment[];
  notes: DisputeNote[];
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

export interface DisputeStatusHistory {
  id: string;
  fromStatus: string | null;
  toStatus: string;
  changedBy: string;
  note?: string | null;
  changedAt: string;
}

export interface DisputeAttachment {
  id: string;
  messageId?: string | null;
  fileName: string;
  contentType: string;
  sizeBytes: number;
  uploadedBy: string;
  createdAt: string;
}

export interface DisputeNote {
  id: string;
  authorId: string;
  content: string;
  createdAt: string;
}

export interface CreateDisputeRequest {
  orderId: string;
  reason: string;
  priority?: DisputePriority;
}

export interface AddDisputeMessageRequest {
  content: string;
}

export interface DisputePage {
  items: Dispute[];
  totalElements: number;
  totalPages: number;
  number: number;
}

export interface DisputeQuery {
  status?: string;
  priority?: string;
  search?: string;
  page?: number;
  size?: number;
}
