import { Component, OnInit } from '@angular/core';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-delivery-management',
  templateUrl: './delivery-management.component.html',
  styleUrls: ['./delivery-management.component.css']
})
export class DeliveryManagementComponent implements OnInit {
  deliveries: Order[] = [];
  loading = true;

  showDeliverModal = false;
  selectedOrder: Order | null = null;
  receivedBy = '';
  delivering = false;

  constructor(private orderService: OrderService, private toast: ToastService) {}

  ngOnInit(): void {
    this.loadDeliveries();
  }

  loadDeliveries(): void {
    this.loading = true;
    this.orderService.myDeliveries().subscribe({
      next: (data: Order[]) => { this.deliveries = data; this.loading = false; },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur de chargement'); this.loading = false; }
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
    this.receivedBy = '';
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
        this.toast.success('Livraison confirmée avec succès');
        this.closeDeliver();
        this.delivering = false;
        this.loadDeliveries();
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur');
        this.delivering = false;
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
