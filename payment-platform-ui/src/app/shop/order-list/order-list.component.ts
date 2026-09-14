import { Component, OnInit, OnDestroy } from '@angular/core';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';
import { ToastService } from '../../services/toast.service';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-order-list',
    templateUrl: './order-list.component.html',
    styleUrls: ['./order-list.component.css'],
    standalone: false
})
export class OrderListComponent implements OnInit, OnDestroy {
  orders: Order[] = [];
  loading = true;
  filterStatus = '';

  private subscriptions = new Subscription();

  constructor(private orderService: OrderService, private toast: ToastService) {}

  ngOnInit(): void { this.load(); }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  load(): void {
    this.loading = true;
    this.subscriptions.add(this.orderService.list().subscribe({
      next: (data: Order[]) => { this.orders = data; this.loading = false; },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur'); this.loading = false; }
    }));
  }

  get filteredOrders(): Order[] {
    if (!this.filterStatus) return this.orders;
    return this.orders.filter(o => o.status === this.filterStatus);
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = {
      DRAFT: 'Brouillon', CONFIRMED: 'Confirmé',
      PREPARING: 'En préparation', READY_FOR_DELIVERY: 'Prêt pour livraison',
      IN_DELIVERY: 'En livraison', DELIVERED: 'Livré', ACCEPTED: 'Accepté',
      CANCELLED: 'Annulé', REJECTED: 'Rejeté', DELIVERY_REJECTED: 'Livraison rejetée'
    };
    return map[s] || s;
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      DRAFT: 'draft', CONFIRMED: 'confirmed',
      PREPARING: 'preparing', READY_FOR_DELIVERY: 'ready',
      IN_DELIVERY: 'delivery', DELIVERED: 'delivered',
      ACCEPTED: 'accepted', CANCELLED: 'cancelled',
      REJECTED: 'rejected', DELIVERY_REJECTED: 'delivery-rejected'
    };
    return map[s] || '';
  }

  accept(id: string): void {
    this.subscriptions.add(this.orderService.accept(id).subscribe({
      next: () => this.load(),
      error: (e: any) => { this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }

  reject(id: string): void {
    if (!confirm('Rejeter cette commande ?')) return;
    this.subscriptions.add(this.orderService.reject(id).subscribe({
      next: () => this.load(),
      error: (e: any) => { this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }

  cancel(id: string): void {
    this.subscriptions.add(this.orderService.cancel(id).subscribe({
      next: () => this.load(),
      error: (e: any) => { this.toast.error(e.error?.message || 'Erreur'); }
    }));
  }
}
