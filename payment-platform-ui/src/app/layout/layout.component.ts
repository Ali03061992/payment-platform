import { Component, HostListener, OnInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from '../services/login.service';
import { NotificationService } from '../services/notification.service';
import { Notification } from '../models/notification.model';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.css']
})
export class LayoutComponent implements OnInit, OnDestroy {
  user: any;
  sidebarOpen = false;
  isMobile = false;
  showNotifications = false;
  unreadCount = 0;
  notifications: Notification[] = [];
  private subs: Subscription[] = [];

  // Swipe gesture tracking
  private touchStartX = 0;
  private touchStartY = 0;
  private touchCurrentX = 0;
  private isSwiping = false;
  private swipeThreshold = 80;

  navItems: { label: string; icon: string; route: string; roles: string[] }[] = [
    { label: 'Tableau de bord', icon: '📊', route: '', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'] },
    { label: 'Gestion des utilisateurs', icon: '👥', route: 'admin/users', roles: ['SYSTEM_ADMIN'] },
    { label: 'Créer un compte', icon: '➕', route: 'admin/users/create', roles: ['SYSTEM_ADMIN'] },
    { label: 'Activation comptes', icon: '🔑', route: 'sales/accounts', roles: ['SYSTEM_ADMIN'] },
    { label: 'Fournisseurs', icon: '🏭', route: 'admin/suppliers', roles: ['SYSTEM_ADMIN'] },
    { label: 'Boutiques', icon: '🏪', route: 'admin/shops', roles: ['SYSTEM_ADMIN'] },
    { label: 'Relations F-B', icon: '🔗', route: 'admin/relations', roles: ['SYSTEM_ADMIN'] },
    { label: 'Stats organisations', icon: '📈', route: 'admin/org-stats', roles: ['SYSTEM_ADMIN'] },
    { label: 'Catalogue', icon: '📋', route: 'supplier/products', roles: ['SUPPLIER_ADMIN'] },
    { label: 'Stock', icon: '📦', route: 'supplier/stock', roles: ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'] },
    { label: 'Commandes', icon: '🛒', route: 'supplier/orders', roles: ['SUPPLIER_ADMIN'] },
    { label: 'Livraisons', icon: '🚚', route: 'supplier/deliveries', roles: ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'] },
    { label: 'Mes commandes', icon: '🛒', route: 'shop/orders', roles: ['SHOP_ADMIN', 'SHOP_MANAGER', 'SHOP_AGENT'] },
    { label: 'Nouvelle commande', icon: '➕', route: 'shop/orders/create', roles: ['SHOP_ADMIN', 'SHOP_MANAGER'] },
    { label: 'Balance', icon: '💰', route: 'shop/balance', roles: ['SHOP_ADMIN', 'SHOP_MANAGER'] },
    { label: 'Paiements', icon: '💰', route: 'payments', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'] },
    { label: 'Stats paiements', icon: '📊', route: 'payments/stats', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'] },
    { label: 'Scanner QR', icon: '📱', route: 'scan', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'] },
    { label: 'Paiements agents', icon: '👥', route: 'supplier/agent-payments', roles: ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'] },
    { label: 'Recherche paiements', icon: '🔍', route: 'payments/search', roles: ['SUPPLIER_ADMIN', 'SYSTEM_ADMIN'] },
  ];

  constructor(
    private loginService: LoginService,
    private router: Router,
    private notificationService: NotificationService
  ) {
    this.user = this.loginService.getCurrentUser();
    this.checkMobile();
  }

  ngOnInit(): void {
    this.notificationService.startPolling(30000);
    this.subs.push(
      this.notificationService.notifications$.subscribe(n => this.notifications = n),
      this.notificationService.unreadCount$.subscribe(c => this.unreadCount = c)
    );
  }

  ngOnDestroy(): void {
    this.subs.forEach(s => s.unsubscribe());
  }

  @HostListener('window:resize')
  onResize() {
    this.checkMobile();
  }

  private checkMobile() {
    this.isMobile = window.innerWidth <= 768;
    if (this.isMobile) {
      this.sidebarOpen = false;
    }
  }

  get filteredNav() {
    return this.navItems.filter(item => item.roles.some(r => this.user?.roles?.includes(r)));
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  onNavClick(): void {
    if (this.isMobile) {
      this.sidebarOpen = false;
    }
  }

  // Swipe gesture handlers for sidebar
  onTouchStart(event: TouchEvent): void {
    if (!this.isMobile) return;
    this.touchStartX = event.touches[0].clientX;
    this.touchStartY = event.touches[0].clientY;
    this.isSwiping = false;
  }

  onTouchMove(event: TouchEvent): void {
    if (!this.isMobile) return;

    this.touchCurrentX = event.touches[0].clientX;
    const deltaY = Math.abs(event.touches[0].clientY - this.touchStartY);
    const deltaX = this.touchCurrentX - this.touchStartX;

    // Only start swiping if horizontal movement is significant and more than vertical
    if (Math.abs(deltaX) > 10 && Math.abs(deltaX) > deltaY) {
      this.isSwiping = true;

      // If swiping right from left edge, open sidebar
      if (deltaX > 0 && this.touchStartX < 30 && !this.sidebarOpen) {
        event.preventDefault();
        this.sidebarOpen = true;
      }
      // If swiping left while sidebar is open, close it
      else if (deltaX < 0 && this.sidebarOpen) {
        event.preventDefault();
        this.sidebarOpen = false;
      }
    }
  }

  onTouchEnd(event: TouchEvent): void {
    if (!this.isMobile) return;
    this.isSwiping = false;
  }

  toggleNotifications(): void {
    this.showNotifications = !this.showNotifications;
    if (this.showNotifications && this.unreadCount > 0) {
      this.markAllRead();
    }
  }

  closeNotifications(): void {
    this.showNotifications = false;
  }

  markAllRead(): void {
    this.notificationService.markAllAsRead().subscribe(res => {
      if (res.updated > 0) {
        this.unreadCount = 0;
        this.notifications = this.notifications.map(n => ({ ...n, readStatus: 'READ' }));
      }
    });
  }

  getNotificationIcon(type: string): string {
    switch (type) {
      case 'PAYMENT_CREATED': return '💸';
      case 'PAYMENT_CONFIRMED': return '✅';
      case 'PAYMENT_REJECTED': return '❌';
      case 'PAYMENT_CANCELLED': return '🚫';
      default: return '🔔';
    }
  }

  navigateNotification(notif: Notification): void {
    if (notif.relatedEntityType === 'PAYMENT' && notif.relatedEntityId) {
      this.router.navigate(['/dashboard/payments'], { queryParams: { ref: notif.relatedEntityId } });
    }
    this.showNotifications = false;
  }

  logout(): void {
    this.loginService.logout();
    this.router.navigate(['/login']);
  }

  getInitials(): string {
    if (!this.user) return '?';
    return (this.user.firstName?.[0] || '') + (this.user.lastName?.[0] || '');
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
}
