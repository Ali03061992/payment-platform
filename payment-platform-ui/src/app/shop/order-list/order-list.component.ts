import { Component, OnInit, OnDestroy, HostListener, ElementRef } from '@angular/core';
import { OrderService } from '../../services/order.service';
import { Order } from '../../models/order.model';
import { ToastService } from '../../services/toast.service';
import { Subscription, Subject, debounceTime, distinctUntilChanged, switchMap, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

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

  searchQuery = '';
  searchResults: Order[] = [];
  showSearchDropdown = false;
  searching = false;
  searchSubject = new Subject<string>();

  private subscriptions = new Subscription();

  constructor(private orderService: OrderService, private toast: ToastService, private elRef: ElementRef) {}

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
      DELIVERY_ACCEPTED: 'Livraison acceptée',
      IN_DELIVERY: 'En livraison', DELIVERED: 'Livré', ACCEPTED: 'Accepté',
      CANCELLED: 'Annulé', REJECTED: 'Rejeté', DELIVERY_REJECTED: 'Livraison rejetée'
    };
    return map[s] || s;
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

  exportCsv(): void {
    this.orderService.exportCsv({ status: this.filterStatus || undefined });
  }
}
