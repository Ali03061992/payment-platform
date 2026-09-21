import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';
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

  statusSteps = ['DRAFT', 'CONFIRMED', 'PREPARING', 'READY_FOR_DELIVERY', 'DELIVERY_ACCEPTED', 'IN_DELIVERY', 'DELIVERED', 'ACCEPTED'];

  private subscriptions = new Subscription();

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private orderService: OrderService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    const id = this.route.snapshot.paramMap.get('id')!;
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(id);
    const request$ = isUuid
      ? this.orderService.getById(id)
      : this.orderService.getByReference(id);
    this.subscriptions.add(request$.subscribe({
      next: (data: Order) => { this.order = data; this.loading = false; },
      error: () => { this.loading = false; this.router.navigate(['/dashboard/shop/orders']); }
    }));
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
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

  isStepCompleted(stepIndex: number): boolean {
    if (!this.order) return false;
    const currentIndex = this.statusSteps.indexOf(this.order.status);
    return currentIndex >= 0 && stepIndex <= currentIndex;
  }

  isCurrentStep(step: string): boolean {
    return this.order?.status === step;
  }
}
