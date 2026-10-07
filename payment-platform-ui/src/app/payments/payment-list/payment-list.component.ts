import { Component, OnInit, OnDestroy } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { PaymentService } from '../../services/payment.service';
import { Payment, PaymentPage } from '../../models/payment.model';
import { ToastService } from '../../services/toast.service';
import { Subscription } from 'rxjs';
import { statusLabelFr } from '../../pipes/status-label.pipe';
import { sortItems, toggleSortState, ariaSortFor, sortIndicatorFor, SortState } from '../../models/page.model';

@Component({
    selector: 'app-payment-list',
    templateUrl: './payment-list.component.html',
    styleUrls: ['./payment-list.component.css'],
    standalone: false
})
export class PaymentListComponent implements OnInit, OnDestroy {
  payments: Payment[] = [];
  loading = true;
  filterStatus = '';
  pendingAction: string | null = null;
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  /** Lignes dépliées (détail inline) : vide à l'initialisation, rien n'est affiché. */
  expandedIds = new Set<string>();
  sort: SortState = { field: null, direction: 'asc' };

  private subscriptions = new Subscription();

  constructor(private paymentService: PaymentService, private toast: ToastService, private translate: TranslateService) {}

  ngOnInit(): void { this.load(); }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  load(): void {
    this.loading = true;
    this.subscriptions.add(this.paymentService.listPaged(this.currentPage, this.pageSize).subscribe({
      next: (page: PaymentPage) => {
        this.payments = page.items || [];
        this.totalElements = page.totalElements ?? this.payments.length;
        this.totalPages = page.totalPages ?? 1;
        this.currentPage = page.number ?? this.currentPage;
        this.expandedIds.clear();
        this.loading = false;
      },
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => { this.toast.error(err.error?.message || this.translate.instant('PAYMENTS.ERROR')); this.loading = false; }
    }));
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  onSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.load();
  }

  onFilterChange(): void {
    this.currentPage = 0;
  }

  isExpanded(p: Payment): boolean {
    return this.expandedIds.has(p.id);
  }

  toggleExpand(p: Payment, event?: Event): void {
    event?.stopPropagation();
    if (this.expandedIds.has(p.id)) {
      this.expandedIds.delete(p.id);
    } else {
      this.expandedIds.add(p.id);
    }
  }

  get filteredPayments(): Payment[] {
    const base = !this.filterStatus ? this.payments : this.payments.filter(p => p.status === this.filterStatus);
    return sortItems(base, this.sort.field, this.sort.direction);
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

  statusLabel(s: string): string {
    return statusLabelFr(s);
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      PENDING: 'pending', CONFIRMED: 'confirmed', REJECTED: 'rejected', CANCELLED: 'cancelled'
    };
    return map[s] || '';
  }

  confirm(id: string): void {
    if (this.pendingAction) return;
    this.pendingAction = id;
    this.subscriptions.add(this.paymentService.confirm(id).subscribe({
      next: () => { this.pendingAction = null; this.load(); },
      error: (e: any) => { this.pendingAction = null; this.toast.error(e.error?.message || this.translate.instant('PAYMENTS.ERROR')); }
    }));
  }

  cancel(id: string): void {
    if (this.pendingAction) return;
    this.pendingAction = id;
    this.subscriptions.add(this.paymentService.cancel(id).subscribe({
      next: () => { this.pendingAction = null; this.load(); },
      error: (e: any) => { this.pendingAction = null; this.toast.error(e.error?.message || this.translate.instant('PAYMENTS.ERROR')); }
    }));
  }

  exportCsv(): void {
    this.paymentService.exportCsv({ status: this.filterStatus || undefined });
  }
}