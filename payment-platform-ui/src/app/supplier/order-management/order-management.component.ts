import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { SupplierAgentService } from '../../services/supplier-agent.service';
import { Order } from '../../models/order.model';
import { ToastService } from '../../services/toast.service';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-order-management',
    templateUrl: './order-management.component.html',
    styleUrls: ['./order-management.component.css'],
    standalone: false
})
export class OrderManagementComponent implements OnInit, OnDestroy {
  orders: Order[] = [];
  deliveries: Order[] = [];
  loading = true;
  filterStatus = '';
  activeTab: 'orders' | 'deliveries' = 'orders';
  filterAgentId = '';

  showDetail = false;
  selectedOrder: Order | null = null;
  loadingDetail = false;

  showAssignModal = false;
  assignOrderId = '';
  assignAgentId = '';
  assignPlannedDate = '';
  assigning = false;
  agents: { id: string; firstName: string; lastName: string }[] = [];

  private subscriptions = new Subscription();

  constructor(
    private orderService: OrderService,
    private agentService: SupplierAgentService,
    private router: Router,
    private route: ActivatedRoute,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['ref']) {
        this.pendingRef = params['ref'];
      }
    });

    this.loadOrders();
    this.loadAgents();
  }

  private pendingRef: string | null = null;

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  loadOrders(): void {
    this.loading = true;
    this.subscriptions.add(this.orderService.list().subscribe({
      next: (data: Order[]) => {
        this.orders = data;
        this.loading = false;
        if (this.pendingRef) {
          const ref = this.pendingRef;
          this.pendingRef = null;
          const order = this.orders.find(o => o.reference === ref || o.id === ref);
          if (order) {
            this.viewDetail(order);
          }
        }
      },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur de chargement'); this.loading = false; }
    }));
  }

  loadDeliveries(agentId?: string): void {
    this.loading = true;
    this.subscriptions.add(this.orderService.listDeliveries(agentId).subscribe({
      next: (data: Order[]) => { this.deliveries = data; this.loading = false; },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur de chargement'); this.loading = false; }
    }));
  }

  loadAgents(): void {
    const userJson = sessionStorage.getItem('user');
    let supplierId = '';
    if (userJson) {
      try { supplierId = JSON.parse(userJson).organizationId || ''; } catch {}
    }
    if (!supplierId) return;
    this.subscriptions.add(this.agentService.listAgents(supplierId).subscribe({
      next: (agents) => {
        this.agents = agents.map((a: any) => ({
          id: a.id,
          firstName: a.firstName,
          lastName: a.lastName
        }));
      },
      error: () => {
        this.agents = [];
      }
    }));
  }

  switchTab(tab: 'orders' | 'deliveries'): void {
    this.activeTab = tab;
    this.filterStatus = '';
    this.filterAgentId = '';
    if (tab === 'deliveries') {
      this.loadDeliveries();
    } else {
      this.loadOrders();
    }
  }

  onAgentFilterChange(): void {
    if (this.filterAgentId) {
      this.loadDeliveries(this.filterAgentId);
    } else {
      this.loadDeliveries();
    }
  }

  get filteredOrders(): Order[] {
    if (!this.filterStatus) return this.orders;
    return this.orders.filter(o => o.status === this.filterStatus);
  }

  get filteredDeliveries(): Order[] {
    if (!this.filterStatus) return this.deliveries;
    return this.deliveries.filter(o => o.status === this.filterStatus);
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = {
      DRAFT: 'Brouillon',
      CONFIRMED: 'Confirmé',
      PREPARING: 'En préparation',
      READY_FOR_DELIVERY: 'Prêt pour livraison',
      DELIVERY_ACCEPTED: 'Livraison acceptée',
      DELIVERY_REJECTED: 'Livraison rejetée',
      IN_DELIVERY: 'En livraison',
      DELIVERED: 'Livré',
      ACCEPTED: 'Accepté',
      CANCELLED: 'Annulé',
      REJECTED: 'Rejeté'
    };
    return map[s] || s;
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      DRAFT: 'draft',
      CONFIRMED: 'confirmed',
      PREPARING: 'preparing',
      READY_FOR_DELIVERY: 'ready',
      DELIVERY_ACCEPTED: 'confirmed',
      DELIVERY_REJECTED: 'rejected',
      IN_DELIVERY: 'in-delivery',
      DELIVERED: 'delivered',
      ACCEPTED: 'accepted',
      CANCELLED: 'cancelled',
      REJECTED: 'rejected'
    };
    return map[s] || '';
  }

  viewDetail(order: Order): void {
    this.loadingDetail = true;
    this.showDetail = true;
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(order.id);
    const request$ = isUuid
      ? this.orderService.getById(order.id)
      : this.orderService.getByReference(order.reference || order.id);
    this.subscriptions.add(request$.subscribe({
      next: (data) => { this.selectedOrder = data; this.loadingDetail = false; },
      error: () => { this.selectedOrder = order; this.loadingDetail = false; }
    }));
  }

  closeDetail(): void {
    this.showDetail = false;
    this.selectedOrder = null;
  }

  confirm(order: Order): void {
    this.subscriptions.add(this.orderService.confirm(order.id).subscribe({
      next: () => { this.toast.success('Commande confirmée'); this.loadOrders(); },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur'); }
    }));
  }

  prepare(order: Order): void {
    this.subscriptions.add(this.orderService.prepare(order.id).subscribe({
      next: () => { this.toast.success('Commande mise en préparation'); this.loadOrders(); },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur'); }
    }));
  }

  ready(order: Order): void {
    this.subscriptions.add(this.orderService.readyForDelivery(order.id).subscribe({
      next: () => { this.toast.success('Commande prête pour livraison'); this.loadOrders(); },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur'); }
    }));
  }

  openAssign(order: Order): void {
    this.assignOrderId = order.id;
    this.assignAgentId = '';
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    this.assignPlannedDate = tomorrow.toISOString().split('T')[0];
    this.showAssignModal = true;
  }

  closeAssign(): void {
    this.showAssignModal = false;
  }

  submitAssign(): void {
    if (!this.assignAgentId) return;
    this.assigning = true;
    this.subscriptions.add(this.orderService.assignDelivery(this.assignOrderId, this.assignAgentId, this.assignPlannedDate).subscribe({
      next: () => {
        this.toast.success('Agent assigné avec succès');
        this.closeAssign();
        this.assigning = false;
        this.loadOrders();
      },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur'); this.assigning = false; }
    }));
  }

  deliveryReject(order: Order): void {
    if (!confirm('Rejeter la livraison de cette commande ?')) return;
    this.subscriptions.add(this.orderService.deliveryReject(order.id).subscribe({
      next: () => { this.toast.success('Livraison rejetée'); this.loadOrders(); },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur'); }
    }));
  }

  cancel(order: Order): void {
    if (!confirm('Annuler cette commande ?')) return;
    this.subscriptions.add(this.orderService.cancel(order.id).subscribe({
      next: () => { this.toast.success('Commande annulée'); this.loadOrders(); },
      error: (err: any) => { this.toast.error(err.error?.message || 'Erreur'); }
    }));
  }

  canConfirm(order: Order): boolean {
    return order.status === 'DRAFT';
  }

  canPrepare(order: Order): boolean {
    return order.status === 'CONFIRMED';
  }

  canReady(order: Order): boolean {
    return order.status === 'PREPARING';
  }

  canAssign(order: Order): boolean {
    return order.status === 'READY_FOR_DELIVERY';
  }

  canDeliveryReject(order: Order): boolean {
    return order.status === 'IN_DELIVERY';
  }

  canCancel(order: Order): boolean {
    return order.status !== 'DELIVERED' && order.status !== 'CANCELLED' && order.status !== 'ACCEPTED' && order.status !== 'REJECTED';
  }
}
