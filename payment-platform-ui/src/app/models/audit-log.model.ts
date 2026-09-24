export interface AuditLogEntry {
  id: string;
  userId: string;
  organizationId: string;
  action: string;
  entityId: string;
  timestamp: string;
  details: string;
}

export interface AuditLogPage {
  content: AuditLogEntry[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}
