import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { NotificationService } from '../../services/notification.service';
import { Notification, NotificationPage } from '../../models/notification.model';
import { Subscription } from 'rxjs';

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
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;
  private subscriptions = new Subscription();

  typeOptions = [
    { value: '', label: 'Tous les types' },
    { value: 'ORDER', label: 'Commandes' },
    { value: 'PAYMENT', label: 'Paiements' },
    { value: 'DELIVERY', label: 'Livraisons' },
  ];

  constructor(
    private notificationService: NotificationService,
    private router: Router
  ) {}

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
      this.router.navigate(['/dashboard/supplier/orders'], { queryParams: { ref: notif.relatedEntityId } });
    } else if (notif.relatedEntityType === 'DELIVERY' && notif.relatedEntityId) {
      this.router.navigate(['/dashboard/supplier/deliveries'], { queryParams: { orderId: notif.relatedEntityId } });
    } else if (notif.relatedEntityType === 'SHOP_ORDER' && notif.relatedEntityId) {
      this.router.navigate(['/dashboard/shop/orders', notif.relatedEntityId]);
    }
  }

  getNotificationIcon(type: string): string {
    switch (type) {
      case 'PAYMENT_CREATED': return '💸';
      case 'PAYMENT_CONFIRMED': return '✅';
      case 'PAYMENT_REJECTED': return '❌';
      case 'PAYMENT_CANCELLED': return '🚫';
      case 'ORDER_CREATED': return '🛒';
      case 'ORDER_CONFIRMED': return '✅';
      case 'ORDER_SHIPPED': return '🚚';
      case 'ORDER_DELIVERED': return '📦';
      case 'DELIVERY_STARTED': return '🚚';
      case 'DELIVERY_COMPLETED': return '📦';
      case 'STOCK_LOW': return '🚨';
      default: return '🔔';
    }
  }

  typeLabel(type: string): string {
    const n = this.notifications.find(x => x.type === type);
    if (n?.relatedEntityType === 'ORDER') return 'Commande';
    if (n?.relatedEntityType === 'PAYMENT') return 'Paiement';
    if (n?.relatedEntityType === 'DELIVERY') return 'Livraison';
    if (n?.relatedEntityType === 'SHOP_ORDER') return 'Commande boutique';
    if (type.includes('PAYMENT')) return 'Paiement';
    if (type.includes('ORDER')) return 'Commande';
    if (type.includes('DELIVERY')) return 'Livraison';
    return 'Autre';
  }

  typeBadgeClass(type: string): string {
    if (type.includes('PAYMENT')) return 'type-payment';
    if (type.includes('ORDER')) return 'type-order';
    if (type.includes('DELIVERY')) return 'type-delivery';
    return 'type-other';
  }

  getTimeAgo(dateStr: string): string {
    const now = Date.now();
    const then = new Date(dateStr).getTime();
    const diff = now - then;
    const mins = Math.floor(diff / 60000);
    if (mins < 1) return 'à l\'instant';
    if (mins < 60) return `il y a ${mins}min`;
    const hours = Math.floor(mins / 60);
    if (hours < 24) return `il y a ${hours}h`;
    const days = Math.floor(hours / 24);
    return `il y a ${days}j`;
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
