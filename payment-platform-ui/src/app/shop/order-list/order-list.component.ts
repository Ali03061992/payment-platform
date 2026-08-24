import { Component, OnInit } from '@angular/core';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';

@Component({
  selector: 'app-order-list',
  templateUrl: './order-list.component.html',
  styleUrls: ['./order-list.component.css']
})
export class OrderListComponent implements OnInit {
  orders: Order[] = [];
  loading = true;
  filterStatus = '';
  errorMsg = '';

  constructor(private orderService: OrderService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.orderService.list().subscribe({
      next: (data: Order[]) => { this.orders = data; this.loading = false; },
      error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur'; this.loading = false; }
    });
  }

  get filteredOrders(): Order[] {
    if (!this.filterStatus) return this.orders;
    return this.orders.filter(o => o.status === this.filterStatus);
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

  accept(id: number): void {
    this.orderService.accept(id).subscribe({
      next: () => this.load(),
      error: (e: any) => { this.errorMsg = e.error?.message || 'Erreur'; setTimeout(() => this.errorMsg = '', 3000); }
    });
  }

  cancel(id: number): void {
    this.orderService.cancel(id).subscribe({
      next: () => this.load(),
      error: (e: any) => { this.errorMsg = e.error?.message || 'Erreur'; setTimeout(() => this.errorMsg = '', 3000); }
    });
  }
}
