export interface Notification {
  id: string;
  recipientUserId: string;
  recipientOrganizationId: string | null;
  type: string;
  message: string;
  readStatus: string;
  createdAt: string;
  readAt: string | null;
  relatedEntityType: string;
  relatedEntityId: string;
}
