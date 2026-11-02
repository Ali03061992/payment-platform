import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Subscription, forkJoin } from 'rxjs';
import { DisputeService } from '../../services/dispute.service';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { Dispute, DisputePage, DisputePriority } from '../../models/dispute.model';
import { Order } from '../../models/order.model';
import { statusLabelFr } from '../../pipes/status-label.pipe';

const REASON_MAX_LENGTH = 255;

/**
 * Page universelle « Réclamations » : liste les litiges de l'organisation
 * et permet à chaque rôle métier d'en ouvrir un nouveau sur une commande
 * éligible (rejetée ou livraison rejetée).
 */
@Component({
    selector: 'app-reclamations',
    templateUrl: './reclamations.component.html',
    styleUrls: ['./reclamations.component.css'],
    standalone: false
})
export class ReclamationsComponent implements OnInit, OnDestroy {
  disputes: Dispute[] = [];
  eligibleOrders: Order[] = [];
  loading = true;
  loadingOrders = false;
  showForm = false;
  selectedOrderId = '';
  reason = '';
  createPriority: DisputePriority = 'NORMAL';
  submitting = false;

  searchTerm = '';
  statusFilter = '';
  priorityFilter = '';
  page = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  readonly priorities: DisputePriority[] = ['LOW', 'NORMAL', 'HIGH', 'URGENT'];
  private subscriptions = new Subscription();

  constructor(
    private disputeService: DisputeService,
    private orderService: OrderService,
    private toast: ToastService,
    private router: Router,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  load(): void {
    this.loading = true;
    this.subscriptions.add(this.disputeService.listPaged({
      status: this.statusFilter || undefined,
      priority: this.priorityFilter || undefined,
      search: this.searchTerm.trim() || undefined,
      page: this.page,
      size: this.pageSize
    }).subscribe({
      next: (result: DisputePage) => {
        this.disputes = result.items || [];
        this.totalElements = result.totalElements ?? this.disputes.length;
        this.totalPages = result.totalPages ?? 0;
        this.page = result.number ?? this.page;
        this.loading = false;
      },
      error: (err: { error?: { message?: string } }) => {
        this.toast.error(err.error?.message || this.translate.instant('RECLAMATIONS.LOAD_ERROR'));
        this.loading = false;
      }
    }));
  }

  onFilterChange(): void {
    this.page = 0;
    this.load();
  }

  resetFilters(): void {
    this.searchTerm = '';
    this.statusFilter = '';
    this.priorityFilter = '';
    this.onFilterChange();
  }

  previousPage(): void {
    if (this.page <= 0) return;
    this.page -= 1;
    this.load();
  }

  nextPage(): void {
    if (this.page + 1 >= this.totalPages) return;
    this.page += 1;
    this.load();
  }

  priorityLabel(priority?: string): string {
    if (!priority) return '';
    return this.translate.instant(`DISPUTES.PRIORITY_${priority}`);
  }

  priorityClass(priority?: string): string {
    return priority ? `priority-${priority.toLowerCase()}` : '';
  }

  isOverdue(d: Dispute): boolean {
    return !!d.dueAt
      && new Date(d.dueAt).getTime() < Date.now()
      && d.status !== 'RESOLVED' && d.status !== 'CLOSED';
  }

  openForm(): void {
    this.showForm = true;
    this.selectedOrderId = '';
    this.reason = '';
    this.createPriority = 'NORMAL';
    this.loadEligibleOrders();
  }

  closeForm(): void {
    this.showForm = false;
  }

  private loadEligibleOrders(): void {
    this.loadingOrders = true;
    this.subscriptions.add(this.orderService.listEligibleForDispute(0, 100).subscribe({
      next: (orders) => {
        this.eligibleOrders = orders || [];
        this.loadingOrders = false;
      },
      error: () => {
        this.eligibleOrders = [];
        this.loadingOrders = false;
        this.toast.error(this.translate.instant('RECLAMATIONS.ORDERS_LOAD_ERROR'));
      }
    }));
  }

  canSubmit(): boolean {
    return !!this.selectedOrderId
      && !!this.reason.trim()
      && this.reason.trim().length <= REASON_MAX_LENGTH
      && !this.submitting;
  }

  create(): void {
    if (!this.canSubmit()) return;
    this.submitting = true;
    this.subscriptions.add(this.disputeService.create({
      orderId: this.selectedOrderId,
      reason: this.reason.trim(),
      priority: this.createPriority
    }).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('RECLAMATIONS.CREATED'));
        this.showForm = false;
        this.submitting = false;
        this.load();
      },
      error: (err: { error?: { message?: string } }) => {
        this.toast.error(err.error?.message || this.translate.instant('RECLAMATIONS.CREATE_ERROR'));
        this.submitting = false;
      }
    }));
  }

  openDispute(id: string): void {
    this.router.navigate(['/dashboard/reclamations', id]);
  }

  orderLabel(o: Order): string {
    return `${o.reference} — ${this.statusLabel(o.status)}`;
  }

  statusLabel(s: string): string {
    const key = `STATUS.${s}`;
    const translated = this.translate.instant(key);
    return translated && translated !== key ? translated : statusLabelFr(s);
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      OPEN: 'pending', IN_PROGRESS: 'confirmed', RESOLVED: 'confirmed', CLOSED: 'cancelled'
    };
    return map[s] || '';
  }
}
