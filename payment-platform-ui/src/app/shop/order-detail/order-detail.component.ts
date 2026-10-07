import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { OrderService } from '../../services/order.service';
import { DisputeService } from '../../services/dispute.service';
import { Order, OrderComment } from '../../models/order.model';
import { Dispute } from '../../models/dispute.model';
import { ToastService } from '../../services/toast.service';
import { ConfirmDialogService } from '../../components/confirm-dialog/confirm-dialog.service';
import { Subscription } from 'rxjs';
import { statusLabelFr } from '../../pipes/status-label.pipe';

@Component({
    selector: 'app-order-detail',
    templateUrl: './order-detail.component.html',
    styleUrls: ['./order-detail.component.css'],
    standalone: false
})
export class ShopOrderDetailComponent implements OnInit, OnDestroy {
  order: Order | null = null;
  loading = true;
  disputes: Dispute[] = [];
  showDisputeForm = false;
  disputeReason = '';
  submittingDispute = false;

  comments: OrderComment[] = [];
  newComment = '';
  submittingComment = false;

  statusSteps = ['DRAFT', 'CONFIRMED', 'PREPARING', 'READY_FOR_DELIVERY', 'DELIVERY_ACCEPTED', 'IN_DELIVERY', 'DELIVERED', 'ACCEPTED'];
  countdown = '';
  private countdownInterval: any;

  private subscriptions = new Subscription();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private orderService: OrderService,
    private disputeService: DisputeService,
    private toast: ToastService,
    private confirmDialog: ConfirmDialogService,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id);
    const request$ = isUuid
      ? this.orderService.getById(id)
      : this.orderService.getByReference(id);
    this.subscriptions.add(request$.subscribe({
      next: (data: Order) => {
        this.order = data;
        this.loading = false;
        this.loadDisputes(data.id);
        this.loadComments(data.id);
        this.startCountdown();
      },
      error: () => { this.loading = false; this.router.navigate(['/dashboard/shop/orders']); }
    }));
  }

  loadDisputes(orderId: string): void {
    this.subscriptions.add(this.disputeService.getByOrder(orderId).subscribe({
      next: (data: Dispute[]) => { this.disputes = data; },
      error: () => {}
    }));
  }

  loadComments(orderId: string): void {
    this.subscriptions.add(this.orderService.getComments(orderId).subscribe({
      next: (data: OrderComment[]) => { this.comments = data; },
      error: () => {}
    }));
  }

  addComment(): void {
    if (!this.order || !this.newComment.trim()) return;
    this.submittingComment = true;
    this.subscriptions.add(this.orderService.addComment(this.order.id, this.newComment.trim()).subscribe({
      next: (comment: OrderComment) => {
        this.comments = [...this.comments, comment];
        this.newComment = '';
        this.submittingComment = false;
        this.toast.success(this.translate.instant('ORDER_DETAIL.COMMENT_ADDED'));
      },
      error: (e: any) => {
        this.submittingComment = false;
        this.toast.error(e.error?.message || this.translate.instant('ORDER_DETAIL.ERROR'));
      }
    }));
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
    if (this.countdownInterval) clearInterval(this.countdownInterval);
  }

  private startCountdown(): void {
    if (this.countdownInterval) clearInterval(this.countdownInterval);
    if (!this.order?.estimatedArrival || this.order.status !== 'IN_DELIVERY') return;
    this.updateCountdown();
    this.countdownInterval = setInterval(() => this.updateCountdown(), 60000);
  }

  private updateCountdown(): void {
    if (!this.order?.estimatedArrival) { this.countdown = ''; return; }
    const now = new Date();
    const eta = new Date(this.order.estimatedArrival);
    const diff = eta.getTime() - now.getTime();
    if (diff <= 0) { this.countdown = this.translate.instant('ORDER_DETAIL.ARRIVING_SOON'); return; }
    const hours = Math.floor(diff / 3600000);
    const mins = Math.floor((diff % 3600000) / 60000);
    this.countdown = hours > 0 ? this.translate.instant('ORDER_DETAIL.COUNTDOWN_HM', { h: hours, m: mins }) : this.translate.instant('ORDER_DETAIL.COUNTDOWN_M', { m: mins });
  }

  accept(): void {
    if (!this.order) return;
    this.subscriptions.add(this.orderService.accept(this.order.id).subscribe({
      next: (data: Order) => { this.order = data; this.toast.success(this.translate.instant('ORDER_DETAIL.ACCEPTED_MSG')); },
      error: (e: any) => { this.toast.error(e.error?.message || this.translate.instant('ORDER_DETAIL.ERROR')); }
    }));
  }

  acceptAsap(): void {
    if (!this.order) return;
    this.subscriptions.add(this.orderService.acceptAsap(this.order.id).subscribe({
      next: (data: Order) => { this.order = data; this.toast.success(this.translate.instant('ORDER_DETAIL.ACCEPTED_ASAP_MSG')); },
      error: (e: any) => { this.toast.error(e.error?.message || this.translate.instant('ORDER_DETAIL.ERROR')); }
    }));
  }

  reject(): void {
    if (!this.order) return;
    const id = this.order.id;
    this.subscriptions.add(this.confirmDialog.confirm({
      title: this.translate.instant('ORDERS.REJECT_ORDER_TITLE'),
      message: this.translate.instant('ORDERS.REJECT_ORDER_MSG'),
      danger: true,
    }).subscribe(ok => {
      if (!ok) return;
      this.subscriptions.add(this.orderService.reject(id).subscribe({
        next: (data: Order) => { this.order = data; this.toast.success(this.translate.instant('ORDER_DETAIL.REJECTED_MSG')); },
        error: (e: any) => { this.toast.error(e.error?.message || this.translate.instant('ORDER_DETAIL.ERROR')); }
      }));
    }));
  }

  cancel(): void {
    if (!this.order) return;
    this.subscriptions.add(this.orderService.cancel(this.order.id).subscribe({
      next: (data: Order) => { this.order = data; this.toast.success(this.translate.instant('ORDER_DETAIL.CANCELLED_MSG')); },
      error: (e: any) => { this.toast.error(e.error?.message || this.translate.instant('ORDER_DETAIL.ERROR')); }
    }));
  }

  toggleDisputeForm(): void {
    this.showDisputeForm = !this.showDisputeForm;
    this.disputeReason = '';
  }

  openDispute(): void {
    if (!this.order || !this.disputeReason.trim()) return;
    this.submittingDispute = true;
    this.subscriptions.add(this.disputeService.create({ orderId: this.order.id, reason: this.disputeReason.trim() }).subscribe({
      next: (data: Dispute) => {
        this.disputes = [data, ...this.disputes];
        this.showDisputeForm = false;
        this.disputeReason = '';
        this.submittingDispute = false;
        this.toast.success(this.translate.instant('DISPUTES.OPENED_MSG'));
        this.router.navigate(['/dashboard/shop/disputes', data.id]);
      },
      error: (e: any) => { this.submittingDispute = false; this.toast.error(e.error?.message || this.translate.instant('ORDER_DETAIL.ERROR')); }
    }));
  }

  viewDispute(disputeId: string): void {
    this.router.navigate(['/dashboard/shop/disputes', disputeId]);
  }

  statusLabel(s: string): string {
    return statusLabelFr(s);
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      DRAFT: 'draft', CONFIRMED: 'confirmed',
      PREPARING: 'preparing', READY_FOR_DELIVERY: 'ready',
      DELIVERY_ACCEPTED: 'confirmed',
      IN_DELIVERY: 'delivery', DELIVERED: 'delivered',
      ACCEPTED: 'accepted', CANCELLED: 'cancelled',
      REJECTED: 'rejected', DELIVERY_REJECTED: 'delivery-rejected'
    };
    return map[s] || '';
  }

  actionLabel(a: string): string {
    const map: Record<string, string> = {
      ORDER_CREATED: 'ORDER_DETAIL.ACTION_CREATED', ORDER_CONFIRMED: 'ORDER_DETAIL.ACTION_CONFIRMED',
      ORDER_PREPARING: 'ORDER_DETAIL.ACTION_PREPARING', ORDER_READY_FOR_DELIVERY: 'ORDER_DETAIL.ACTION_READY',
      ORDER_IN_DELIVERY: 'ORDER_DETAIL.ACTION_IN_DELIVERY', ORDER_DELIVERED: 'ORDER_DETAIL.ACTION_DELIVERED',
      ORDER_ACCEPTED: 'ORDER_DETAIL.ACTION_ACCEPTED', ORDER_CANCELLED: 'ORDER_DETAIL.ACTION_CANCELLED',
      ORDER_ACCEPTED_ASAP: 'ORDER_DETAIL.ACTION_ACCEPTED_ASAP',
      ORDER_REJECTED: 'ORDER_DETAIL.ACTION_REJECTED', ORDER_DELIVERY_REJECTED: 'ORDER_DETAIL.ACTION_DELIVERY_REJECTED'
    };
    return this.translate.instant(map[a] || a);
  }

  printOrder(): void {
    window.print();
  }

  downloadInvoice(): void {
    if (!this.order) return;
    this.orderService.downloadInvoice(this.order.id);
  }

  paymentTermsLabel(terms: string): string {
    const map: Record<string, string> = {
      IMMEDIATE: 'ORDER_CREATE.PAY_IMMEDIATE',
      NET_15: 'ORDER_CREATE.PAY_NET_15',
      NET_30: 'ORDER_CREATE.PAY_NET_30',
      NET_60: 'ORDER_CREATE.PAY_NET_60'
    };
    return this.translate.instant(map[terms] || terms);
  }

  isOverdue(): boolean {
    if (!this.order?.dueDate || this.order.status === 'CANCELLED' || this.order.status === 'REJECTED') {
      return false;
    }
    return new Date(this.order.dueDate) < new Date();
  }

  isStepCompleted(stepIndex: number): boolean {
    if (!this.order) return false;
    const currentIndex = this.statusSteps.indexOf(this.order.status);
    return currentIndex >= 0 && stepIndex <= currentIndex;
  }

  isCurrentStep(step: string): boolean {
    return this.order?.status === step;
  }
}