import { Component, HostListener, OnInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from '../services/login.service';
import { NotificationService } from '../services/notification.service';
import { StockService } from '../services/stock.service';
import { ThemeService, Theme } from '../services/theme.service';
import { Notification } from '../models/notification.model';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-layout',
    templateUrl: './layout.component.html',
    styleUrls: ['./layout.component.css'],
    standalone: false
})
export class LayoutComponent implements OnInit, OnDestroy {
  user: any;
  sidebarOpen = false;
  isMobile = false;
  showNotifications = false;
  unreadCount = 0;
  lowStockAlertCount = 0;
  notifications: Notification[] = [];
  private subs: Subscription[] = [];

  // Swipe gesture tracking
  private touchStartX = 0;
  private touchStartY = 0;
  private touchCurrentX = 0;
  private isSwiping = false;
  private swipeThreshold = 80;

  navItems: { label: string; icon: string; route: string; roles: string[] }[] = [
    { label: 'Tableau de bord', icon: 'dashboard', route: '/dashboard', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'] },
    { label: 'Gestion des utilisateurs', icon: 'users', route: 'admin/users', roles: ['SYSTEM_ADMIN'] },
    { label: 'Créer un compte', icon: 'user-plus', route: 'admin/users/create', roles: ['SYSTEM_ADMIN'] },
    { label: 'Activation comptes', icon: 'key', route: 'sales/accounts', roles: ['SYSTEM_ADMIN'] },
    { label: 'Fournisseurs', icon: 'factory', route: 'admin/suppliers', roles: ['SYSTEM_ADMIN'] },
    { label: 'Boutiques', icon: 'store', route: 'admin/shops', roles: ['SYSTEM_ADMIN'] },
    { label: 'Relations F-B', icon: 'link', route: 'admin/relations', roles: ['SYSTEM_ADMIN'] },
    { label: 'Stats organisations', icon: 'chart', route: 'admin/org-stats', roles: ['SYSTEM_ADMIN'] },
    { label: 'Journal d\'audit', icon: 'list', route: 'admin/audit-logs', roles: ['SYSTEM_ADMIN'] },
    { label: 'Categories', icon: 'tag', route: 'supplier/categories', roles: ['SUPPLIER_ADMIN'] },
    { label: 'Familles', icon: 'folder', route: 'supplier/families', roles: ['SUPPLIER_ADMIN'] },
    { label: 'Produits', icon: 'list', route: 'supplier/products', roles: ['SUPPLIER_ADMIN'] },
    { label: 'Stock', icon: 'box', route: 'supplier/stock', roles: ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'] },
    { label: 'Alertes stock', icon: 'alert', route: 'supplier/low-stock-alerts', roles: ['SUPPLIER_ADMIN'] },
    { label: 'Optimisation', icon: 'cpu', route: 'supplier/optimization', roles: ['SUPPLIER_ADMIN'] },
    { label: 'Commandes', icon: 'cart', route: 'supplier/orders', roles: ['SUPPLIER_ADMIN'] },
    { label: 'Finance', icon: 'wallet', route: 'supplier/financial', roles: ['SUPPLIER_ADMIN'] },
    { label: 'Balance', icon: 'scale', route: 'supplier/balance', roles: ['SUPPLIER_ADMIN'] },
    { label: 'Livraisons', icon: 'truck', route: 'supplier/deliveries', roles: ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'] },
    { label: 'Mes commandes', icon: 'cart', route: 'shop/orders', roles: ['SHOP_ADMIN', 'SHOP_AGENT'] },
    { label: 'Livraisons', icon: 'truck', route: 'shop/deliveries', roles: ['SHOP_ADMIN', 'SHOP_AGENT'] },
    { label: 'Nouvelle commande', icon: 'plus', route: 'shop/orders/create', roles: ['SHOP_ADMIN'] },
    { label: 'Balance', icon: 'scale', route: 'shop/balance', roles: ['SHOP_ADMIN'] },
    { label: 'Paiements', icon: 'card', route: 'payments', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'] },
    { label: 'Stats paiements', icon: 'chart', route: 'payments/stats', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'] },
    { label: 'Scanner QR', icon: 'scan', route: 'scan', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'] },
    { label: 'Export', icon: 'upload', route: 'export', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'] },
    { label: 'Paiements agents', icon: 'users', route: 'supplier/agent-payments', roles: ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'] },
    { label: 'Notifications', icon: 'bell', route: 'notifications', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'] },
    { label: 'Changer mot de passe', icon: 'lock', route: 'change-password', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'] },
  ];

  constructor(
    private loginService: LoginService,
    private router: Router,
    private notificationService: NotificationService,
    private stockService: StockService,
    public themeService: ThemeService
  ) {
    this.user = this.loginService.getCurrentUser();
    this.checkMobile();
  }

  ngOnInit(): void {
    this.notificationService.fetchNotifications().subscribe();
    this.notificationService.fetchUnreadCount().subscribe();
    this.notificationService.startRealtime();
    this.subs.push(
      this.notificationService.notifications$.subscribe(n => this.notifications = n),
      this.notificationService.unreadCount$.subscribe(c => this.unreadCount = c)
    );
    if (this.user?.roles?.some(r => r === 'SUPPLIER_ADMIN' || r === 'SUPPLIER_AGENT')) {
      this.stockService.getLowStockAlerts().subscribe({
        next: (products) => this.lowStockAlertCount = products.length,
        error: () => {}
      });
    }
  }

  ngOnDestroy(): void {
    this.notificationService.stopRealtime();
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

  /** Indique si l'utilisateur connecté est un profil boutique. */
  isShopUser(): boolean {
    const roles = this.user?.roles || [];
    return roles.includes('SHOP_ADMIN') || roles.includes('SHOP_AGENT');
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
      case 'PAYMENT_CREATED': return 'cash';
      case 'PAYMENT_CONFIRMED': return 'check-circle';
      case 'PAYMENT_REJECTED': return 'x-circle';
      case 'PAYMENT_CANCELLED': return 'ban';
      default: return 'bell';
    }
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
