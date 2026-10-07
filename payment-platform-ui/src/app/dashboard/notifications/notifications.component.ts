import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { TranslateService } from '@ngx-translate/core';
import { LoginService } from '../../services/login.service';
import { NotificationService } from '../../services/notification.service';
import { Notification, NotificationPage } from '../../models/notification.model';
import { Subscription } from 'rxjs';
import { timeAgo } from '../../pipes/time-ago.pipe';
import { sortItems, toggleSortState, ariaSortFor, sortIndicatorFor, SortState } from '../../models/page.model';

@Component({
  selector: 'app-notifications',
  templateUrl: './notifications.component.html',
  styleUrls: ['./notifications.component.css'],
  standalone: false
})
export class NotificationsComponent implements OnInit, OnDestroy {
  notifications: Notification[] = [];
  loading = true;
  filterType = '';
  currentPage = 0;
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  sort: SortState = { field: null, direction: 'asc' };
  private subscriptions = new Subscription();

  typeOptions = [
    { value: '', label: 'Tous les types' },
    { value: 'ORDER', label: 'Commandes' },
    { value: 'PAYMENT', label: 'Paiements' },
    { value: 'DELIVERY', label: 'Livraisons' },
  ];

  constructor(
    private notificationService: NotificationService,
    private router: Router,
    private translate: TranslateService,
    private loginService: LoginService
  ) {}

  /** Boutique (admin/agent) : les pages fournisseur rendraient 403. */
  isShopUser(): boolean {
    const roles = this.loginService.getCurrentUser()?.roles || [];
    return roles.includes('SHOP_ADMIN') || roles.includes('SHOP_AGENT');
  }

  ngOnInit(): void {
    this.load();
  }

  ngOnDestroy(): void {
    this.subscriptions.unsubscribe();
  }

  load(): void {
    this.loading = true;
    this.subscriptions.add(
      this.notificationService.fetchNotificationsPaged(
        this.currentPage,
        this.pageSize,
        this.filterType || undefined
      ).subscribe({
        next: (page: NotificationPage) => {
          this.notifications = page.items || [];
          this.totalElements = page.totalElements;
          this.totalPages = page.totalPages;
          this.currentPage = page.currentPage;
          this.loading = false;
        },
        error: () => {
          this.notifications = [];
          this.loading = false;
        }
      })
    );
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

  onSort(field: string): void {
    this.sort = toggleSortState(this.sort, field);
  }

  ariaSort(field: string): 'ascending' | 'descending' | 'none' {
    return ariaSortFor(field, this.sort);
  }

  sortIndicator(field: string): string {
    return sortIndicatorFor(field, this.sort);
  }

  get sortedNotifications(): Notification[] {
    return sortItems(this.notifications, this.sort.field, this.sort.direction);
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.load();
    }
  }

  prevPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.load();
    }
  }

  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.load();
    }
  }

  markAsRead(id: string, event: Event): void {
    event.stopPropagation();
    this.subscriptions.add(
      this.notificationService.markAsRead(id).subscribe({
        next: () => {
          const n = this.notifications.find(x => x.id === id);
          if (n) {
            n.readStatus = 'READ';
          }
          this.notificationService.fetchUnreadCount().subscribe();
        }
      })
    );
  }

  markAllRead(): void {
    this.subscriptions.add(
      this.notificationService.markAllAsRead().subscribe({
        next: () => {
          this.notifications = this.notifications.map(n => ({ ...n, readStatus: 'READ' }));
          this.notificationService.fetchUnreadCount().subscribe();
        }
      })
    );
  }

  navigateNotification(notif: Notification): void {
    if (notif.relatedEntityType === 'PAYMENT' && notif.relatedEntityId) {
      this.router.navigate(['/dashboard/payments', notif.relatedEntityId]);
    } else if (notif.relatedEntityType === 'ORDER' && notif.relatedEntityId) {
      // Boutique : détail commande boutique (la liste fournisseur rendrait 403).
      if (this.isShopUser()) {
        this.router.navigate(['/dashboard/shop/orders', notif.relatedEntityId]);
      } else {
        this.router.navigate(['/dashboard/supplier/orders'], { queryParams: { ref: notif.relatedEntityId } });
      }
    } else if (notif.relatedEntityType === 'DELIVERY' && notif.relatedEntityId) {
      // Boutique : page livraisons boutique (la page fournisseur rendrait 403).
      const target = this.isShopUser() ? '/dashboard/shop/deliveries' : '/dashboard/supplier/deliveries';
      this.router.navigate([target], { queryParams: { orderId: notif.relatedEntityId } });
    } else if (notif.relatedEntityType === 'SHOP_ORDER' && notif.relatedEntityId) {
      this.router.navigate(['/dashboard/shop/orders', notif.relatedEntityId]);
    } else if (notif.relatedEntityType === 'DISPUTE' && notif.relatedEntityId) {
      const roles = this.loginService.getCurrentUser()?.roles || [];
      if (roles.includes('SYSTEM_ADMIN')) {
        this.router.navigate(['/dashboard/admin/disputes', notif.relatedEntityId]);
      } else if (this.isShopUser()) {
        this.router.navigate(['/dashboard/shop/disputes', notif.relatedEntityId]);
      } else {
        this.router.navigate(['/dashboard/supplier/orders']);
      }
    }
  }

  getNotificationIcon(type: string): string {
    switch (type) {
      case 'PAYMENT_CREATED': return 'cash';
      case 'PAYMENT_CONFIRMED': return 'check-circle';
      case 'PAYMENT_REJECTED': return 'x-circle';
      case 'PAYMENT_CANCELLED': return 'ban';
      case 'ORDER_CREATED': return 'cart';
      case 'ORDER_CONFIRMED': return 'check-circle';
      case 'ORDER_SHIPPED': return 'truck';
      case 'ORDER_DELIVERED': return 'box';
      case 'DELIVERY_STARTED': return 'truck';
      case 'DELIVERY_COMPLETED': return 'box';
      case 'STOCK_LOW': return 'alert';
      default: return 'bell';
    }
  }

  typeLabel(type: string): string {
    const n = this.notifications.find(x => x.type === type);
    if (n?.relatedEntityType === 'ORDER') return this.translate.instant('NOTIFICATIONS.TYPE_ORDER');
    if (n?.relatedEntityType === 'PAYMENT') return this.translate.instant('NOTIFICATIONS.TYPE_PAYMENT');
    if (n?.relatedEntityType === 'DELIVERY') return this.translate.instant('NOTIFICATIONS.TYPE_DELIVERY');
    if (n?.relatedEntityType === 'SHOP_ORDER') return this.translate.instant('NOTIFICATIONS.TYPE_SHOP_ORDER');
    if (type.includes('PAYMENT')) return this.translate.instant('NOTIFICATIONS.TYPE_PAYMENT');
    if (type.includes('ORDER')) return this.translate.instant('NOTIFICATIONS.TYPE_ORDER');
    if (type.includes('DELIVERY')) return this.translate.instant('NOTIFICATIONS.TYPE_DELIVERY');
    return this.translate.instant('NOTIFICATIONS.TYPE_OTHER');
  }

  typeBadgeClass(type: string): string {
    if (type.includes('PAYMENT')) return 'type-payment';
    if (type.includes('ORDER')) return 'type-order';
    if (type.includes('DELIVERY')) return 'type-delivery';
    return 'type-other';
  }

  getTimeAgo(dateStr: string): string {
    return timeAgo(dateStr);
  }

  get unreadCount(): number {
    return this.notifications.filter(n => n.readStatus === 'UNREAD').length;
  }

  get pageNumbers(): number[] {
    const pages: number[] = [];
    const maxVisible = 5;
    let start = Math.max(0, this.currentPage - Math.floor(maxVisible / 2));
    let end = Math.min(this.totalPages, start + maxVisible);
    if (end - start < maxVisible) {
      start = Math.max(0, end - maxVisible);
    }
    for (let i = start; i < end; i++) {
      pages.push(i);
    }
    return pages;
  }
}