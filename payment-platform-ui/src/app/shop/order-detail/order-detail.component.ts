import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { DisputeService } from '../../services/dispute.service';
import { Order, OrderComment } from '../../models/order.model';
import { Dispute } from '../../models/dispute.model';
import { ToastService } from '../../services/toast.service';
import { Subscription } from 'rxjs';

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
    private toast: ToastService
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
        this.toast.success('Commentaire ajouté');
      },
      error: (e: any) => {
        this.submittingComment = false;
        this.toast.error(e.error?.message || 'Erreur');
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
    if (diff <= 0) { this.countdown = 'Arrivée imminente'; return; }
    const hours = Math.floor(diff / 3600000);
    const mins = Math.floor((diff % 3600000) / 60000);
    this.countdown = hours > 0 ? `≈ ${hours}h ${mins}min` : `≈ ${mins} min`;
  }

  accept(): void {
    if (!this.order) return;
    this.subscriptions.add(this.orderService.accept(this.order.id).subscribe({
      next: (data: Order) => { this.order = data; this.toast.success('Commande acceptée'); },
      error: (e: any) => { this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }

  acceptAsap(): void {
    if (!this.order) return;
    this.subscriptions.add(this.orderService.acceptAsap(this.order.id).subscribe({
      next: (data: Order) => { this.order = data; this.toast.success('Commande acceptée avec paiement ASAP'); },
      error: (e: any) => { this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }

  reject(): void {
    if (!this.order) return;
    if (!confirm('Rejeter cette commande ?')) return;
    this.subscriptions.add(this.orderService.reject(this.order.id).subscribe({
      next: (data: Order) => { this.order = data; this.toast.success('Commande rejetée'); },
      error: (e: any) => { this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }

  cancel(): void {
    if (!this.order) return;
    this.subscriptions.add(this.orderService.cancel(this.order.id).subscribe({
      next: (data: Order) => { this.order = data; this.toast.success('Commande annulée'); },
      error: (e: any) => { this.toast.error(e.error?.message || 'Erreur'); }
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
        this.toast.success('Litige ouvert');
        this.router.navigate(['/dashboard/shop/disputes', data.id]);
      },
      error: (e: any) => { this.submittingDispute = false; this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }

  viewDispute(disputeId: string): void {
    this.router.navigate(['/dashboard/shop/disputes', disputeId]);
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = {
      DRAFT: 'Brouillon', CONFIRMED: 'Confirmé',
      PREPARING: 'En préparation', READY_FOR_DELIVERY: 'Prêt pour livraison',
      DELIVERY_ACCEPTED: 'Livraison acceptée',
      IN_DELIVERY: 'En livraison', DELIVERED: 'Livré', ACCEPTED: 'Accepté',
      CANCELLED: 'Annulé', REJECTED: 'Rejeté', DELIVERY_REJECTED: 'Livraison rejetée'
    };
    return map[s] || s;
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
      ORDER_CREATED: 'Créé', ORDER_CONFIRMED: 'Confirmé',
      ORDER_PREPARING: 'En préparation', ORDER_READY_FOR_DELIVERY: 'Prêt',
      ORDER_IN_DELIVERY: 'En livraison', ORDER_DELIVERED: 'Livré',
      ORDER_ACCEPTED: 'Accepté', ORDER_CANCELLED: 'Annulé',
      ORDER_ACCEPTED_ASAP: 'Accepté (ASAP)',
      ORDER_REJECTED: 'Rejeté', ORDER_DELIVERY_REJECTED: 'Livraison rejetée'
    };
    return map[a] || a;
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
      IMMEDIATE: 'Immédiat',
      NET_15: 'Net 15 jours',
      NET_30: 'Net 30 jours',
      NET_60: 'Net 60 jours'
    };
    return map[terms] || terms;
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
