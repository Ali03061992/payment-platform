import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { AuditLogService } from '../../services/audit-log.service';
import { AuditLogEntry, AuditLogPage } from '../../models/audit-log.model';
import { sortItems, toggleSortState, ariaSortFor, sortIndicatorFor, SortState } from '../../models/page.model';

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
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  sort: SortState = { field: null, direction: 'asc' };

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

  constructor(private auditLogService: AuditLogService, private translate: TranslateService) {}

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
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => {
        this.error = err.error?.message || this.translate.instant('AUDIT.LOAD_ERROR');
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

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadLogs();
  }

  onSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadLogs();
  }

  onSort(field: string): void {
    this.sort = toggleSortState(this.sort, field);
  }

  ariaSort(field: string): 'ascending' | 'descending' | 'none' {
    return ariaSortFor(field, this.sort);
  }

  sortIndicator(field: string): string {
    return sortIndicatorFor(field, this.sort);
  }

  get sortedLogs(): AuditLogEntry[] {
    return sortItems(this.logs, this.sort.field, this.sort.direction);
  }

  formatAction(action: string): string {
    return action.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
  }
}
