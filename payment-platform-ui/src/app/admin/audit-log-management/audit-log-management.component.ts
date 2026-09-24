import { Component, OnInit } from '@angular/core';
import { AuditLogService } from '../../services/audit-log.service';
import { AuditLogEntry, AuditLogPage } from '../../models/audit-log.model';

@Component({
    selector: 'app-audit-log-management',
    templateUrl: './audit-log-management.component.html',
    styleUrls: ['./audit-log-management.component.css'],
    standalone: false
})
export class AuditLogManagementComponent implements OnInit {
  logs: AuditLogEntry[] = [];
  loading = true;
  error = '';
  currentPage = 0;
  pageSize = 50;
  totalElements = 0;
  totalPages = 0;

  filterUserId = '';
  filterAction = '';
  filterDateFrom = '';
  filterDateTo = '';

  availableActions = [
    'PAYMENT_CREATED', 'PAYMENT_CONFIRMED', 'PAYMENT_REJECTED', 'PAYMENT_CANCELLED',
    'SUPPLIER_CREATED', 'SUPPLIER_ENABLED', 'SUPPLIER_DISABLED',
    'SHOP_CREATED', 'SHOP_ENABLED', 'SHOP_DISABLED',
    'USER_CREATED', 'USER_ENABLED', 'USER_DISABLED',
    'USER_LOGIN', 'USER_PASSWORD_CHANGE'
  ];

  constructor(private auditLogService: AuditLogService) {}

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.loading = true;
    this.error = '';
    this.auditLogService.list({
      page: this.currentPage,
      size: this.pageSize,
      userId: this.filterUserId || undefined,
      action: this.filterAction || undefined,
      dateFrom: this.filterDateFrom || undefined,
      dateTo: this.filterDateTo || undefined
    }).subscribe({
      next: (page: AuditLogPage) => {
        this.logs = page.content;
        this.totalElements = page.totalElements;
        this.totalPages = page.totalPages;
        this.currentPage = page.number;
        this.loading = false;
      },
      error: (err: any) => {
        this.error = err.error?.message || 'Erreur de chargement';
        this.loading = false;
      }
    });
  }

  applyFilter(): void {
    this.currentPage = 0;
    this.loadLogs();
  }

  clearFilters(): void {
    this.filterUserId = '';
    this.filterAction = '';
    this.filterDateFrom = '';
    this.filterDateTo = '';
    this.currentPage = 0;
    this.loadLogs();
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadLogs();
    }
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadLogs();
    }
  }

  formatAction(action: string): string {
    return action.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  }
}
