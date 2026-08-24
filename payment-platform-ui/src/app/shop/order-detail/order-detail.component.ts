import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';

@Component({
  selector: 'app-order-detail',
  templateUrl: './order-detail.component.html',
  styleUrls: ['./order-detail.component.css']
})
export class ShopOrderDetailComponent implements OnInit {
  order: Order | null = null;
  loading = true;
  errorMsg = '';
  successMsg = '';

  statusSteps = ['DRAFT', 'PENDING', 'CONFIRMED', 'PREPARING', 'READY_FOR_DELIVERY', 'IN_DELIVERY', 'DELIVERED', 'ACCEPTED'];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private orderService: OrderService
  ) {}

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.orderService.getById(id).subscribe({
      next: (data: Order) => { this.order = data; this.loading = false; },
      error: () => { this.loading = false; this.router.navigate(['/dashboard/shop/orders']); }
    });
  }

  accept(): void {
    if (!this.order) return;
    this.orderService.accept(this.order.id).subscribe({
      next: (data: Order) => { this.order = data; this.successMsg = 'Commande acceptée'; },
      error: (e: any) => { this.errorMsg = e.error?.message || 'Erreur'; setTimeout(() => this.errorMsg = '', 3000); }
    });
  }

  acceptAsap(): void {
    if (!this.order) return;
    this.orderService.acceptAsap(this.order.id).subscribe({
      next: (data: Order) => { this.order = data; this.successMsg = 'Commande acceptée avec paiement ASAP'; },
      error: (e: any) => { this.errorMsg = e.error?.message || 'Erreur'; setTimeout(() => this.errorMsg = '', 3000); }
    });
  }

  cancel(): void {
    if (!this.order) return;
    this.orderService.cancel(this.order.id).subscribe({
      next: (data: Order) => { this.order = data; this.successMsg = 'Commande annulée'; },
      error: (e: any) => { this.errorMsg = e.error?.message || 'Erreur'; setTimeout(() => this.errorMsg = '', 3000); }
    });
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = {
      DRAFT: 'Brouillon', PENDING: 'En attente', CONFIRMED: 'Confirmé',
      PREPARING: 'En préparation', READY_FOR_DELIVERY: 'Prêt pour livraison',
      IN_DELIVERY: 'En livraison', DELIVERED: 'Livré', ACCEPTED: 'Accepté',
      CANCELLED: 'Annulé'
    };
    return map[s] || s;
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      DRAFT: 'draft', PENDING: 'pending', CONFIRMED: 'confirmed',
      PREPARING: 'preparing', READY_FOR_DELIVERY: 'ready',
      IN_DELIVERY: 'delivery', DELIVERED: 'delivered',
      ACCEPTED: 'accepted', CANCELLED: 'cancelled'
    };
    return map[s] || '';
  }

  actionLabel(a: string): string {
    const map: Record<string, string> = {
      ORDER_CREATED: 'Créé', ORDER_CONFIRMED: 'Confirmé',
      ORDER_PREPARING: 'En préparation', ORDER_READY_FOR_DELIVERY: 'Prêt',
      ORDER_IN_DELIVERY: 'En livraison', ORDER_DELIVERED: 'Livré',
      ORDER_ACCEPTED: 'Accepté', ORDER_CANCELLED: 'Annulé',
      ORDER_ACCEPTED_ASAP: 'Accepté (ASAP)'
    };
    return map[a] || a;
  }

  isStepCompleted(stepIndex: number): boolean {
    if (!this.order) return false;
    const currentIndex = this.statusSteps.indexOf(this.order.status);
    return stepIndex <= currentIndex;
  }

  isCurrentStep(step: string): boolean {
    return this.order?.status === step;
  }
}
