import { Component, OnInit, OnDestroy, HostListener, ElementRef } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { OrderService } from '../../services/order.service';
import { SupplierAgentService } from '../../services/supplier-agent.service';
import { StockService } from '../../services/stock.service';
import { Order, OrderComment } from '../../models/order.model';
import { ToastService } from '../../services/toast.service';
import { Subscription, Subject, debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

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
  filterShopId = '';
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

  searchQuery = '';
  searchResults: Order[] = [];
  showSearchDropdown = false;
  searching = false;
  searchSubject = new Subject<string>();

  showEditModal = false;
  editOrderId = '';
  editNotes = '';
  editAsapPayment = false;
  editOrderLines: { productId: string; productName: string; quantity: number; discount: number; unitPrice: number }[] = [];
  editProducts: any[] = [];
  editSearchQuery = '';
  editing = false;
  loadingEditProducts = false;

  detailComments: OrderComment[] = [];
  detailNewComment = '';
  detailSubmittingComment = false;

  private subscriptions = new Subscription();

  constructor(
    private orderService: OrderService,
    private agentService: SupplierAgentService,
    private stockService: StockService,
    private router: Router,
    private route: ActivatedRoute,
    private toast: ToastService,
    private elRef: ElementRef
  ) {}

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elRef.nativeElement.querySelector('.search-container')?.contains(event.target)) {
      this.showSearchDropdown = false;
    }
  }

  ngOnInit(): void {
    this.route.queryParams.subscribe(params => {
      if (params['ref']) {
        this.pendingRef = params['ref'];
      }
    });

    this.subscriptions.add(
      this.searchSubject.pipe(
        debounceTime(300),
        distinctUntilChanged(),
        switchMap(query => {
          if (!query || query.trim().length < 2) {
            this.searchResults = [];
            this.showSearchDropdown = false;
            this.searching = false;
            return of([]);
          }
          this.searching = true;
          return this.orderService.search(query.trim()).pipe(
            catchError(() => { this.searching = false; return of([]); })
          );
        })
      ).subscribe(results => {
        this.searchResults = results;
        this.showSearchDropdown = results.length > 0;
        this.searching = false;
      })
    );

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
    this.filterShopId = '';
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

  get shops(): { id: string; name: string }[] {
    const allOrders = [...this.orders, ...this.deliveries];
    const map = new Map<string, string>();
    for (const o of allOrders) {
      if (o.shopId && !map.has(o.shopId)) {
        map.set(o.shopId, o.shopName || o.shopId);
      }
    }
    return Array.from(map.entries()).map(([id, name]) => ({ id, name }));
  }

  get filteredOrders(): Order[] {
    let result = this.orders;
    if (this.filterStatus) {
      result = result.filter(o => o.status === this.filterStatus);
    }
    if (this.filterShopId) {
      result = result.filter(o => o.shopId === this.filterShopId);
    }
    return result;
  }

  get filteredDeliveries(): Order[] {
    let result = this.deliveries;
    if (this.filterStatus) {
      result = result.filter(o => o.status === this.filterStatus);
    }
    if (this.filterShopId) {
      result = result.filter(o => o.shopId === this.filterShopId);
    }
    return result;
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
    this.showSearchDropdown = false;
    this.loadingDetail = true;
    this.showDetail = true;
    this.detailComments = [];
    this.detailNewComment = '';
    const isUuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(order.id);
    const request$ = isUuid
      ? this.orderService.getById(order.id)
      : this.orderService.getByReference(order.reference || order.id);
    this.subscriptions.add(request$.subscribe({
      next: (data) => {
        this.selectedOrder = data;
        this.loadingDetail = false;
        this.loadDetailComments(data.id);
      },
      error: () => { this.selectedOrder = order; this.loadingDetail = false; }
    }));
  }

  loadDetailComments(orderId: string): void {
    this.subscriptions.add(this.orderService.getComments(orderId).subscribe({
      next: (data: OrderComment[]) => { this.detailComments = data; },
      error: () => {}
    }));
  }

  addDetailComment(): void {
    if (!this.selectedOrder || !this.detailNewComment.trim()) return;
    this.detailSubmittingComment = true;
    this.subscriptions.add(this.orderService.addComment(this.selectedOrder.id, this.detailNewComment.trim()).subscribe({
      next: (comment: OrderComment) => {
        this.detailComments = [...this.detailComments, comment];
        this.detailNewComment = '';
        this.detailSubmittingComment = false;
        this.toast.success('Commentaire ajouté');
      },
      error: (e: any) => {
        this.detailSubmittingComment = false;
        this.toast.error(e.error?.message || 'Erreur');
      }
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

  downloadInvoice(order: Order): void {
    this.orderService.downloadInvoice(order.id);
  }

  exportCsv(): void {
    this.orderService.exportCsv({ status: this.filterStatus || undefined });
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

  canEdit(order: Order): boolean {
    return order.status === 'DRAFT';
  }

  openEdit(order: Order): void {
    this.editOrderId = order.id;
    this.editNotes = order.notes || '';
    this.editAsapPayment = order.asapPayment;
    this.editOrderLines = (order.items || []).map(item => ({
      productId: item.productId,
      productName: item.productName,
      quantity: item.quantity,
      discount: item.discount || 0,
      unitPrice: item.unitPrice
    }));
    this.editSearchQuery = '';
    this.showEditModal = true;
    this.loadEditProducts();
  }

  closeEdit(): void {
    this.showEditModal = false;
    this.editOrderId = '';
    this.editOrderLines = [];
  }

  loadEditProducts(): void {
    this.loadingEditProducts = true;
    this.subscriptions.add(this.stockService.getProducts('ACTIVE').subscribe({
      next: (data) => {
        this.editProducts = data.filter(p => p.status === 'ACTIVE');
        this.loadingEditProducts = false;
      },
      error: () => {
        this.editProducts = [];
        this.loadingEditProducts = false;
      }
    }));
  }

  get filteredEditProducts(): any[] {
    if (!this.editSearchQuery) return this.editProducts;
    const q = this.editSearchQuery.toLowerCase();
    return this.editProducts.filter(p =>
      p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q)
    );
  }

  addEditProduct(product: any): void {
    const existing = this.editOrderLines.find(l => l.productId === product.id);
    if (existing) {
      existing.quantity++;
    } else {
      this.editOrderLines.push({
        productId: product.id,
        productName: product.name,
        quantity: 1,
        discount: 0,
        unitPrice: product.unitPrice
      });
    }
  }

  removeEditLine(index: number): void {
    this.editOrderLines.splice(index, 1);
  }

  canSubmitEdit(): boolean {
    return this.editOrderLines.length > 0 && !this.editing;
  }

  submitEdit(): void {
    if (!this.canSubmitEdit()) return;
    this.editing = true;
    const request = {
      notes: this.editNotes,
      asapPayment: this.editAsapPayment,
      items: this.editOrderLines.map(l => ({
        productId: l.productId,
        quantity: l.quantity,
        discount: l.discount
      }))
    };
    this.subscriptions.add(this.orderService.update(this.editOrderId, request).subscribe({
      next: () => {
        this.toast.success('Commande modifiée avec succès');
        this.closeEdit();
        this.editing = false;
        this.loadOrders();
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur lors de la modification');
        this.editing = false;
      }
    }));
  }
}
