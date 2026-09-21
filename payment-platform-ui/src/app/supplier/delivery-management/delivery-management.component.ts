import { Component, OnInit } from '@angular/core';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';
import { ToastService } from '../../services/toast.service';

@Component({
    selector: 'app-delivery-management',
    templateUrl: './delivery-management.component.html',
    styleUrls: ['./delivery-management.component.css'],
    standalone: false
})
export class DeliveryManagementComponent implements OnInit {
  deliveries: Order[] = [];
  loading = true;

  showDeliverModal = false;
  selectedOrder: Order | null = null;
  receivedBy = '';
  delivering = false;
  shopAgents: {id: string, name: string}[] = [];

  showConfirmDateModal = false;
  confirmDateOrder: Order | null = null;
  confirmedDate = '';
  confirming = false;

  showRejectModal = false;
  rejectOrder: Order | null = null;
  rejectReason = '';
  rejecting = false;

  constructor(private orderService: OrderService, private toast: ToastService) {}

  ngOnInit(): void {
    this.loadDeliveries();
  }

  loadDeliveries(): void {
    this.loading = true;
    this.orderService.myDeliveries().subscribe({
      next: (data: Order[]) => {
        this.deliveries = data;
        this.loading = false;
      },
      error: (err: any) => {
        console.error('Deliveries error:', err);
        this.toast.error(err.error?.message || 'Erreur de chargement');
        this.loading = false;
      }
    });
  }

  get pendingAcceptance(): Order[] {
    return this.deliveries.filter(d => d.status === 'READY_FOR_DELIVERY');
  }

  get activeDeliveries(): Order[] {
    return this.deliveries.filter(d => d.status === 'DELIVERY_ACCEPTED' || d.status === 'IN_DELIVERY');
  }

  get completedDeliveries(): Order[] {
    return this.deliveries.filter(d => d.status === 'DELIVERED' || d.status === 'ACCEPTED');
  }

  get rejectedDeliveries(): Order[] {
    return this.deliveries.filter(d => d.status === 'DELIVERY_REJECTED');
  }

  acceptDelivery(order: Order): void {
    this.orderService.acceptDelivery(order.id, true).subscribe({
      next: () => {
        this.toast.success('Livraison acceptée');
        this.loadDeliveries();
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur');
      }
    });
  }

  openRejectModal(order: Order): void {
    this.rejectOrder = order;
    this.rejectReason = '';
    this.showRejectModal = true;
  }

  closeRejectModal(): void {
    this.showRejectModal = false;
    this.rejectOrder = null;
    this.rejectReason = '';
  }

  submitReject(): void {
    if (!this.rejectOrder) return;
    this.rejecting = true;
    this.orderService.acceptDelivery(this.rejectOrder.id, false, this.rejectReason).subscribe({
      next: () => {
        this.toast.success('Livraison rejetée');
        this.closeRejectModal();
        this.rejecting = false;
        this.loadDeliveries();
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur');
        this.rejecting = false;
      }
    });
  }

  openConfirmDate(order: Order): void {
    this.confirmDateOrder = order;
    this.confirmedDate = order.plannedDeliveryDate || '';
    this.showConfirmDateModal = true;
  }

  closeConfirmDate(): void {
    this.showConfirmDateModal = false;
    this.confirmDateOrder = null;
  }

  submitConfirmDate(): void {
    if (!this.confirmDateOrder || !this.confirmedDate) return;
    this.confirming = true;
    this.orderService.confirmDelivery(this.confirmDateOrder.id, this.confirmedDate).subscribe({
      next: () => {
        this.toast.success('Date de livraison confirmée');
        this.closeConfirmDate();
        this.confirming = false;
        this.loadDeliveries();
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur');
        this.confirming = false;
      }
    });
  }

  openDeliver(order: Order): void {
    this.selectedOrder = order;
    this.receivedBy = '';
    this.shopAgents = [];
    this.showDeliverModal = true;
    if (order.shopId) {
      this.orderService.getShopAgents(order.shopId).subscribe({
        next: (agents) => this.shopAgents = agents,
        error: () => this.shopAgents = []
      });
    }
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
      READY_FOR_DELIVERY: 'En attente d\'acceptation',
      DELIVERY_ACCEPTED: 'Acceptée - En attente de livraison',
      IN_DELIVERY: 'En livraison',
      DELIVERED: 'Livré',
      ACCEPTED: 'Accepté par la boutique',
      DELIVERY_REJECTED: 'Rejetée'
    };
    return map[s] || s;
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      READY_FOR_DELIVERY: 'pending',
      DELIVERY_ACCEPTED: 'confirmed',
      IN_DELIVERY: 'in-delivery',
      DELIVERED: 'delivered',
      ACCEPTED: 'accepted',
      DELIVERY_REJECTED: 'rejected'
    };
    return map[s] || '';
  }
}
