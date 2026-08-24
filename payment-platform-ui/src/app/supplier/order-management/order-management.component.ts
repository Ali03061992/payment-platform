import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';

@Component({
  selector: 'app-order-management',
  templateUrl: './order-management.component.html',
  styleUrls: ['./order-management.component.css']
})
export class OrderManagementComponent implements OnInit {
  orders: Order[] = [];
  loading = true;
  errorMsg = '';
  successMsg = '';
  filterStatus = '';

  showDetail = false;
  selectedOrder: Order | null = null;
  loadingDetail = false;

  showAssignModal = false;
  assignOrderId = 0;
  assignAgentId = 0;
  assigning = false;

  constructor(
    private orderService: OrderService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders(): void {
    this.loading = true;
    this.orderService.list().subscribe({
      next: (data: Order[]) => { this.orders = data; this.loading = false; },
      error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur de chargement'; this.loading = false; }
    });
  }

  get filteredOrders(): Order[] {
    if (!this.filterStatus) return this.orders;
    return this.orders.filter(o => o.status === this.filterStatus);
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = {
      DRAFT: 'Brouillon',
      CONFIRMÉ: 'Confirmé',
      CONFIRMED: 'Confirmé',
      PREPARING: 'En préparation',
      READY: 'Prêt',
      IN_DELIVERY: 'En livraison',
      DELIVERED: 'Livré',
      CANCELLED: 'Annulé'
    };
    return map[s] || s;
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      DRAFT: 'draft',
      CONFIRMÉ: 'confirmed',
      CONFIRMED: 'confirmed',
      PREPARING: 'preparing',
      READY: 'ready',
      IN_DELIVERY: 'in-delivery',
      DELIVERED: 'delivered',
      CANCELLED: 'cancelled'
    };
    return map[s] || '';
  }

  viewDetail(order: Order): void {
    this.loadingDetail = true;
    this.showDetail = true;
    this.orderService.getById(order.id).subscribe({
      next: (data) => { this.selectedOrder = data; this.loadingDetail = false; },
      error: () => { this.selectedOrder = order; this.loadingDetail = false; }
    });
  }

  closeDetail(): void {
    this.showDetail = false;
    this.selectedOrder = null;
  }

  prepare(order: Order): void {
    this.orderService.prepare(order.id).subscribe({
      next: () => { this.successMsg = 'Commande mise en préparation'; this.loadOrders(); setTimeout(() => this.successMsg = '', 3000); },
      error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur'; setTimeout(() => this.errorMsg = '', 3000); }
    });
  }

  ready(order: Order): void {
    this.orderService.readyForDelivery(order.id).subscribe({
      next: () => { this.successMsg = 'Commande prête pour livraison'; this.loadOrders(); setTimeout(() => this.successMsg = '', 3000); },
      error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur'; setTimeout(() => this.errorMsg = '', 3000); }
    });
  }

  openAssign(order: Order): void {
    this.assignOrderId = order.id;
    this.assignAgentId = 0;
    this.showAssignModal = true;
  }

  closeAssign(): void {
    this.showAssignModal = false;
  }

  submitAssign(): void {
    if (!this.assignAgentId) return;
    this.assigning = true;
    this.orderService.assignDelivery(this.assignOrderId, this.assignAgentId).subscribe({
      next: () => {
        this.successMsg = 'Agent assigné avec succès';
        this.closeAssign();
        this.assigning = false;
        this.loadOrders();
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur'; this.assigning = false; setTimeout(() => this.errorMsg = '', 3000); }
    });
  }

  cancel(order: Order): void {
    if (!confirm('Annuler cette commande ?')) return;
    this.orderService.cancel(order.id).subscribe({
      next: () => { this.successMsg = 'Commande annulée'; this.loadOrders(); setTimeout(() => this.successMsg = '', 3000); },
      error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur'; setTimeout(() => this.errorMsg = '', 3000); }
    });
  }

  canPrepare(order: Order): boolean {
    return order.status === 'CONFIRMED';
  }

  canReady(order: Order): boolean {
    return order.status === 'PREPARING';
  }

  canAssign(order: Order): boolean {
    return order.status === 'READY';
  }

  canCancel(order: Order): boolean {
    return order.status !== 'DELIVERED' && order.status !== 'CANCELLED';
  }
}
