import { Component, OnInit } from '@angular/core';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';

@Component({
  selector: 'app-delivery-management',
  templateUrl: './delivery-management.component.html',
  styleUrls: ['./delivery-management.component.css']
})
export class DeliveryManagementComponent implements OnInit {
  deliveries: Order[] = [];
  loading = true;
  errorMsg = '';
  successMsg = '';

  showDeliverModal = false;
  selectedOrder: Order | null = null;
  receivedBy = 0;
  delivering = false;

  constructor(private orderService: OrderService) {}

  ngOnInit(): void {
    this.loadDeliveries();
  }

  loadDeliveries(): void {
    this.loading = true;
    this.orderService.myDeliveries().subscribe({
      next: (data: Order[]) => { this.deliveries = data; this.loading = false; },
      error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur de chargement'; this.loading = false; }
    });
  }

  get pendingDeliveries(): Order[] {
    return this.deliveries.filter(d => d.status === 'IN_DELIVERY');
  }

  get completedDeliveries(): Order[] {
    return this.deliveries.filter(d => d.status === 'DELIVERED');
  }

  openDeliver(order: Order): void {
    this.selectedOrder = order;
    this.receivedBy = 0;
    this.showDeliverModal = true;
  }

  closeDeliver(): void {
    this.showDeliverModal = false;
    this.selectedOrder = null;
  }

  confirmDeliver(): void {
    if (!this.selectedOrder || !this.receivedBy) return;
    this.delivering = true;
    this.orderService.deliver(this.selectedOrder.id, this.receivedBy).subscribe({
      next: () => {
        this.successMsg = 'Livraison confirmée avec succès';
        this.closeDeliver();
        this.delivering = false;
        this.loadDeliveries();
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: (err: any) => {
        this.errorMsg = err.error?.message || 'Erreur';
        this.delivering = false;
        setTimeout(() => this.errorMsg = '', 3000);
      }
    });
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = {
      IN_DELIVERY: 'En livraison',
      DELIVERED: 'Livré'
    };
    return map[s] || s;
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      IN_DELIVERY: 'in-delivery',
      DELIVERED: 'delivered'
    };
    return map[s] || '';
  }
}
