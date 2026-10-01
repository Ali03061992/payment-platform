import { Component, OnInit, OnDestroy } from '@angular/core';
import { PaymentService } from '../../services/payment.service';
import { Payment, PaymentPage } from '../../models/payment.model';
import { ToastService } from '../../services/toast.service';
import { Subscription } from 'rxjs';

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
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;
  /** Lignes dépliées (détail inline) : vide à l'initialisation, rien n'est affiché. */
  expandedIds = new Set<string>();

  private subscriptions = new Subscription();

  constructor(private paymentService: PaymentService, private toast: ToastService) {}

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
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur'); this.loading = false; }
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
    if (!this.filterStatus) return this.payments;
    return this.payments.filter(p => p.status === this.filterStatus);
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = {
      PENDING: 'En attente', CONFIRMED: 'Confirmé', REJECTED: 'Rejeté', CANCELLED: 'Annulé'
    };
    return map[s] || s;
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
      error: (e: any) => { this.pendingAction = null; this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }

  cancel(id: string): void {
    if (this.pendingAction) return;
    this.pendingAction = id;
    this.subscriptions.add(this.paymentService.cancel(id).subscribe({
      next: () => { this.pendingAction = null; this.load(); },
      error: (e: any) => { this.pendingAction = null; this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }

  exportCsv(): void {
    this.paymentService.exportCsv({ status: this.filterStatus || undefined });
  }
}
