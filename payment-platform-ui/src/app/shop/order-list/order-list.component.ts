import { Component, OnInit, OnDestroy, HostListener, ElementRef } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { OrderService } from '../../services/order.service';
import { Order, OrderPage } from '../../models/order.model';
import { ToastService } from '../../services/toast.service';
import { ConfirmDialogService } from '../../components/confirm-dialog/confirm-dialog.service';
import { Subscription, Subject, debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { statusLabelFr } from '../../pipes/status-label.pipe';
import { sortItems, toggleSortState, ariaSortFor, sortIndicatorFor, SortState } from '../../models/page.model';

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
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  sort: SortState = { field: null, direction: 'asc' };

  searchQuery = '';
  searchResults: Order[] = [];
  showSearchDropdown = false;
  searching = false;
  searchSubject = new Subject<string>();

  private subscriptions = new Subscription();

  constructor(private orderService: OrderService, private toast: ToastService, private confirmDialog: ConfirmDialogService, private elRef: ElementRef, private translate: TranslateService) {}

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: MouseEvent): void {
    if (!this.elRef.nativeElement.querySelector('.search-container')?.contains(event.target)) {
      this.showSearchDropdown = false;
    }
  }

  ngOnInit(): void {
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
    this.load();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  load(): void {
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
      },
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => { this.toast.error(err.error?.message || this.translate.instant('ORDERS.ERROR')); this.loading = false; }
    }));
  }

  onFilterChange(): void {
    this.currentPage = 0;
    this.load();
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  onSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.load();
  }

  get filteredOrders(): Order[] {
    const base = !this.filterStatus ? this.orders : this.orders.filter(o => o.status === this.filterStatus);
    return sortItems(base, this.sort.field, this.sort.direction);
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

  statusLabel(s: string): string {
    return statusLabelFr(s);
  }

  statusClass(s: string): string {
    const map: Record<string, string> = {
      DRAFT: 'draft', CONFIRMED: 'confirmed',
      PREPARING: 'preparing', READY_FOR_DELIVERY: 'ready',
      DELIVERY_ACCEPTED: 'confirmed',
      IN_DELIVERY: 'delivery', DELIVERED: 'delivered',
      ACCEPTED: 'accepted', CANCELLED: 'cancelled',
      REJECTED: 'rejected', DELIVERY_REJECTED: 'delivery-rejected'
    };
    return map[s] || '';
  }

  accept(id: string): void {
    this.subscriptions.add(this.orderService.accept(id).subscribe({
      next: () => this.load(),
      error: (e: any) => { this.toast.error(e.error?.message || this.translate.instant('ORDERS.ERROR')); }
    }));
  }

  reject(id: string): void {
    this.subscriptions.add(this.confirmDialog.confirm({
      title: this.translate.instant('ORDERS.REJECT_ORDER_TITLE'),
      message: this.translate.instant('ORDERS.REJECT_ORDER_MSG'),
      danger: true,
    }).subscribe(ok => {
      if (!ok) return;
      this.subscriptions.add(this.orderService.reject(id).subscribe({
        next: () => this.load(),
        error: (e: any) => { this.toast.error(e.error?.message || this.translate.instant('ORDERS.ERROR')); }
      }));
    }));
  }

  cancel(id: string): void {
    this.subscriptions.add(this.orderService.cancel(id).subscribe({
      next: () => this.load(),
      error: (e: any) => { this.toast.error(e.error?.message || this.translate.instant('ORDERS.ERROR')); }
    }));
  }

  exportCsv(): void {
    this.orderService.exportCsv({ status: this.filterStatus || undefined });
  }
}