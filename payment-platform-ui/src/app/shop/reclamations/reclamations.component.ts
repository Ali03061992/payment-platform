import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { Subscription, forkJoin } from 'rxjs';
import { DisputeService } from '../../services/dispute.service';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { Dispute } from '../../models/dispute.model';
import { Order } from '../../models/order.model';
import { statusLabelFr } from '../../pipes/status-label.pipe';

const REASON_MAX_LENGTH = 255;
const ELIGIBLE_ORDER_STATUSES = ['REJECTED', 'DELIVERY_REJECTED'];

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
  submitting = false;
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
    this.subscriptions.add(this.disputeService.list().subscribe({
      next: (all) => {
        this.disputes = all || [];
        this.loading = false;
      },
      error: (err: { error?: { message?: string } }) => {
        this.toast.error(err.error?.message || this.translate.instant('RECLAMATIONS.LOAD_ERROR'));
        this.loading = false;
      }
    }));
  }

  openForm(): void {
    this.showForm = true;
    this.selectedOrderId = '';
    this.reason = '';
    this.loadEligibleOrders();
  }

  closeForm(): void {
    this.showForm = false;
  }

  private loadEligibleOrders(): void {
    this.loadingOrders = true;
    this.subscriptions.add(this.orderService.list(0, 100).subscribe({
      next: (orders) => {
        this.eligibleOrders = (orders || []).filter((o) =>
          ELIGIBLE_ORDER_STATUSES.includes(o.status));
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
      reason: this.reason.trim()
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
