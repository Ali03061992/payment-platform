export interface Notification {
  id: number;
  recipientUserId: number;
  recipientOrganizationId: number | null;
  type: string;
  message: string;
  readStatus: string;
  createdAt: string;
  readAt: string | null;
  relatedEntityType: string;
  relatedEntityId: string;
}
