import { Component, OnInit, OnDestroy, HostListener, ElementRef } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { OrderService } from '../../services/order.service';
import { SupplierAgentService } from '../../services/supplier-agent.service';
import { StockService } from '../../services/stock.service';
import { Order, OrderComment, OrderPage } from '../../models/order.model';
import { paginateItems, sortItems, toggleSortState, ariaSortFor, sortIndicatorFor, SortState } from '../../models/page.model';
import { ToastService } from '../../services/toast.service';
import { ConfirmDialogService } from '../../components/confirm-dialog/confirm-dialog.service';
import { Subscription, Subject, debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { statusLabelFr } from '../../pipes/status-label.pipe';

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
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  deliveriesPage = 0;
  deliveriesSize = 10;
  sort: SortState = { field: null, direction: 'asc' };
  deliverySort: SortState = { field: null, direction: 'asc' };

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
    private confirmDialog: ConfirmDialogService,
    private elRef: ElementRef,
    private translate: TranslateService
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
    this.subscriptions.add(this.orderService.listPaged(
      this.currentPage, this.pageSize, this.filterStatus || undefined
    ).subscribe({
      next: (page: OrderPage) => {
        this.orders = page.items || [];
        this.totalElements = page.totalElements ?? this.orders.length;
        this.totalPages = page.totalPages ?? 1;
        this.currentPage = page.number ?? this.currentPage;
        this.loading = false;
        if (this.pendingRef) {
          const ref = this.pendingRef;
          this.pendingRef = null;
          const order = this.orders.find(o => o.reference === ref || o.id === ref);
          if (order) {
            this.viewDetail(order);
          } else {
            // La commande ciblée n'est pas dans la page chargée : appel ciblé par référence.
            this.subscriptions.add(this.orderService.getByReference(ref).subscribe({
              next: (o) => this.viewDetail(o),
              error: () => {}
            }));
          }
        }
      },
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => { this.toast.error(err.error?.message || this.translate.instant('SUPPLIER_ORDERS.LOAD_ERROR')); this.loading = false; }
    }));
  }

  loadDeliveries(agentId?: string): void {
    this.loading = true;
    this.subscriptions.add(this.orderService.listDeliveries(agentId).subscribe({
      next: (data: Order[]) => { this.deliveries = data; this.loading = false; },
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => { this.toast.error(err.error?.message || this.translate.instant('SUPPLIER_ORDERS.LOAD_ERROR')); this.loading = false; }
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
    this.currentPage = 0;
    this.deliveriesPage = 0;
    if (tab === 'deliveries') {
      this.loadDeliveries();
    } else {
      this.loadOrders();
    }
  }

  onOrdersFilterChange(): void {
    this.currentPage = 0;
    if (this.activeTab === 'orders') this.loadOrders();
  }

  onDeliveriesFilterChange(): void {
    this.deliveriesPage = 0;
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.loadOrders();
  }

  onSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.loadOrders();
  }

  onDeliveriesPageChange(page: number): void {
    this.deliveriesPage = page;
  }

  onDeliveriesSizeChange(size: number): void {
    this.deliveriesSize = size;
    this.deliveriesPage = 0;
  }

  get pagedDeliveries(): Order[] {
    return paginateItems(this.sortedDeliveries, this.deliveriesPage, this.deliveriesSize);
  }

  onSort(field: string): void {
    this.sort = toggleSortState(this.sort, field);
  }

  ariaSort(field: string): 'ascending' | 'descending' | 'none' {
    return ariaSortFor(field, this.sort);
  }

  sortIndicator(field: string): string {
    return sortIndicatorFor(field, this.sort);
  }

  onDeliverySort(field: string): void {
    this.deliverySort = toggleSortState(this.deliverySort, field);
    this.deliveriesPage = 0;
  }

  deliveryAriaSort(field: string): 'ascending' | 'descending' | 'none' {
    return ariaSortFor(field, this.deliverySort);
  }

  deliverySortIndicator(field: string): string {
    return sortIndicatorFor(field, this.deliverySort);
  }

  get sortedDeliveries(): Order[] {
    return sortItems(this.filteredDeliveries, this.deliverySort.field, this.deliverySort.direction);
  }

  get deliveriesTotal(): number {
    return this.filteredDeliveries.length;
  }

  onAgentFilterChange(): void {
    this.deliveriesPage = 0;
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
    return sortItems(result, this.sort.field, this.sort.direction);
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
    return statusLabelFr(s);
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
        this.toast.success(this.translate.instant('SUPPLIER_ORDERS.COMMENT_ADDED'));
      },
      error: (e: any) => {
        this.detailSubmittingComment = false;
        this.toast.error(e.error?.message || this.translate.instant('PAYMENTS.ERROR'));
      }
    }));
  }

  closeDetail(): void {
    this.showDetail = false;
    this.selectedOrder = null;
  }

  confirm(order: Order): void {
    this.subscriptions.add(this.orderService.confirm(order.id).subscribe({
      next: () => { this.toast.success(this.translate.instant('SUPPLIER_ORDERS.ORDER_CONFIRMED')); this.loadOrders(); },
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => { this.toast.error(err.error?.message || this.translate.instant('PAYMENTS.ERROR')); }
    }));
  }

  prepare(order: Order): void {
    this.subscriptions.add(this.orderService.prepare(order.id).subscribe({
      next: () => { this.toast.success(this.translate.instant('SUPPLIER_ORDERS.ORDER_PREPARING')); this.loadOrders(); },
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => { this.toast.error(err.error?.message || this.translate.instant('PAYMENTS.ERROR')); }
    }));
  }

  ready(order: Order): void {
    this.subscriptions.add(this.orderService.readyForDelivery(order.id).subscribe({
      next: () => { this.toast.success(this.translate.instant('SUPPLIER_ORDERS.ORDER_READY')); this.loadOrders(); },
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => { this.toast.error(err.error?.message || this.translate.instant('PAYMENTS.ERROR')); }
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
        this.toast.success(this.translate.instant('SUPPLIER_ORDERS.AGENT_ASSIGNED'));
        this.closeAssign();
        this.assigning = false;
        this.loadOrders();
      },
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => { this.toast.error(err.error?.message || this.translate.instant('PAYMENTS.ERROR')); this.assigning = false; }
    }));
  }

  deliveryReject(order: Order): void {
    this.subscriptions.add(this.confirmDialog.confirm({
      title: this.translate.instant('SUPPLIER_ORDERS.REJECT_DELIVERY_TITLE'),
      message: this.translate.instant('SUPPLIER_ORDERS.REJECT_DELIVERY_MSG', { ref: order.reference }),
      danger: true,
    }).subscribe(ok => {
      if (!ok) return;
      this.subscriptions.add(this.orderService.deliveryReject(order.id).subscribe({
        next: () => { this.toast.success(this.translate.instant('SUPPLIER_ORDERS.DELIVERY_REJECTED_SUCCESS')); this.loadOrders(); },
        error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => { this.toast.error(err.error?.message || this.translate.instant('PAYMENTS.ERROR')); }
      }));
    }));
  }

  cancel(order: Order): void {
    this.subscriptions.add(this.confirmDialog.confirm({
      title: this.translate.instant('SUPPLIER_ORDERS.CANCEL_TITLE'),
      message: this.translate.instant('SUPPLIER_ORDERS.CANCEL_MSG', { ref: order.reference }),
      danger: true,
    }).subscribe(ok => {
      if (!ok) return;
      this.subscriptions.add(this.orderService.cancel(order.id).subscribe({
        next: () => { this.toast.success(this.translate.instant('SUPPLIER_ORDERS.ORDER_CANCELLED')); this.loadOrders(); },
        error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => { this.toast.error(err.error?.message || this.translate.instant('PAYMENTS.ERROR')); }
      }));
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
        this.toast.success(this.translate.instant('SUPPLIER_ORDERS.ORDER_UPDATED'));
        this.closeEdit();
        this.editing = false;
        this.loadOrders();
      },
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => {
        this.toast.error(err.error?.message || this.translate.instant('SUPPLIER_ORDERS.UPDATE_ERROR'));
        this.editing = false;
      }
    }));
  }
}