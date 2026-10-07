import { Component, HostListener, OnInit, OnDestroy, ElementRef, ViewChild } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from '../services/login.service';
import { NotificationService } from '../services/notification.service';
import { StockService } from '../services/stock.service';
import { ThemeService, Theme } from '../services/theme.service';
import { Notification } from '../models/notification.model';
import { User } from '../models/user.model';
import { Subscription } from 'rxjs';
import { timeAgo } from '../pipes/time-ago.pipe';

interface NavItem {
  label: string;
  icon: string;
  route: string;
  roles: string[];
  group: string;
}

interface NavGroup {
  label: string;
  items: NavItem[];
}

@Component({
    selector: 'app-layout',
    templateUrl: './layout.component.html',
    styleUrls: ['./layout.component.css'],
    standalone: false
})
export class LayoutComponent implements OnInit, OnDestroy {
  user: User | null = null;
  sidebarOpen = false;
  isMobile = false;
  showNotifications = false;
  showThemePicker = false;
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

  navItems: NavItem[] = [
    { label: 'NAV.DASHBOARD', icon: 'dashboard', route: '/dashboard', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'], group: 'NAV.GROUP_MAIN' },
    { label: 'NAV.USER_MANAGEMENT', icon: 'users', route: 'admin/users', roles: ['SYSTEM_ADMIN'], group: 'NAV.GROUP_ADMIN' },
    { label: 'NAV.CREATE_ACCOUNT', icon: 'user-plus', route: 'admin/users/create', roles: ['SYSTEM_ADMIN'], group: 'NAV.GROUP_ADMIN' },
    { label: 'NAV.ACCOUNT_ACTIVATION', icon: 'key', route: 'sales/accounts', roles: ['SYSTEM_ADMIN'], group: 'NAV.GROUP_ADMIN' },
    { label: 'NAV.SUPPLIERS', icon: 'factory', route: 'admin/suppliers', roles: ['SYSTEM_ADMIN'], group: 'NAV.GROUP_ADMIN' },
    { label: 'NAV.SHOPS', icon: 'store', route: 'admin/shops', roles: ['SYSTEM_ADMIN'], group: 'NAV.GROUP_ADMIN' },
    { label: 'NAV.RELATIONS', icon: 'link', route: 'admin/relations', roles: ['SYSTEM_ADMIN'], group: 'NAV.GROUP_ADMIN' },
    { label: 'NAV.ORG_STATS', icon: 'chart', route: 'admin/org-stats', roles: ['SYSTEM_ADMIN'], group: 'NAV.GROUP_ADMIN' },
    { label: 'NAV.AUDIT_LOG', icon: 'list', route: 'admin/audit-logs', roles: ['SYSTEM_ADMIN'], group: 'NAV.GROUP_ADMIN' },
    { label: 'NAV.DISPUTES', icon: 'alert', route: 'admin/disputes', roles: ['SYSTEM_ADMIN'], group: 'NAV.GROUP_ADMIN' },
    { label: 'NAV.CATEGORIES', icon: 'tag', route: 'supplier/categories', roles: ['SUPPLIER_ADMIN'], group: 'NAV.GROUP_CATALOG' },
    { label: 'NAV.FAMILIES', icon: 'folder', route: 'supplier/families', roles: ['SUPPLIER_ADMIN'], group: 'NAV.GROUP_CATALOG' },
    { label: 'NAV.PRODUCTS', icon: 'list', route: 'supplier/products', roles: ['SUPPLIER_ADMIN'], group: 'NAV.GROUP_CATALOG' },
    { label: 'NAV.STOCK', icon: 'box', route: 'supplier/stock', roles: ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'], group: 'NAV.GROUP_STOCK' },
    { label: 'NAV.LOW_STOCK_ALERTS', icon: 'alert', route: 'supplier/low-stock-alerts', roles: ['SUPPLIER_ADMIN'], group: 'NAV.GROUP_STOCK' },
    { label: 'NAV.OPTIMIZATION', icon: 'cpu', route: 'supplier/optimization', roles: ['SUPPLIER_ADMIN'], group: 'NAV.GROUP_STOCK' },
    { label: 'NAV.ORDERS', icon: 'cart', route: 'supplier/orders', roles: ['SUPPLIER_ADMIN'], group: 'NAV.GROUP_SALES' },
    { label: 'NAV.FINANCE', icon: 'wallet', route: 'supplier/financial', roles: ['SUPPLIER_ADMIN'], group: 'NAV.GROUP_FINANCE' },
    { label: 'NAV.BALANCE', icon: 'scale', route: 'supplier/balance', roles: ['SUPPLIER_ADMIN'], group: 'NAV.GROUP_FINANCE' },
    { label: 'NAV.DELIVERIES', icon: 'truck', route: 'supplier/deliveries', roles: ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'], group: 'NAV.GROUP_SALES' },
    { label: 'NAV.MY_ORDERS', icon: 'cart', route: 'shop/orders', roles: ['SHOP_ADMIN', 'SHOP_AGENT'], group: 'NAV.GROUP_SHOP' },
    { label: 'NAV.DELIVERIES', icon: 'truck', route: 'shop/deliveries', roles: ['SHOP_ADMIN', 'SHOP_AGENT'], group: 'NAV.GROUP_SHOP' },
    { label: 'NAV.NEW_ORDER', icon: 'plus', route: 'shop/orders/create', roles: ['SHOP_ADMIN'], group: 'NAV.GROUP_SHOP' },
    { label: 'NAV.BALANCE', icon: 'scale', route: 'shop/balance', roles: ['SHOP_ADMIN'], group: 'NAV.GROUP_SHOP' },
    { label: 'NAV.DISPUTES', icon: 'alert', route: 'shop/disputes', roles: ['SHOP_ADMIN', 'SHOP_AGENT'], group: 'NAV.GROUP_SHOP' },
    { label: 'NAV.PAYMENTS', icon: 'card', route: 'payments', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'], group: 'NAV.GROUP_PAYMENTS' },
    { label: 'NAV.PAYMENT_STATS', icon: 'chart', route: 'payments/stats', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'], group: 'NAV.GROUP_PAYMENTS' },
    { label: 'NAV.QR_SCANNER', icon: 'scan', route: 'scan', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'], group: 'NAV.GROUP_TOOLS' },
    { label: 'NAV.EXPORT', icon: 'upload', route: 'export', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'], group: 'NAV.GROUP_TOOLS' },
    { label: 'NAV.AGENT_PAYMENTS', icon: 'users', route: 'supplier/agent-payments', roles: ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'], group: 'NAV.GROUP_SALES' },
    { label: 'LAYOUT.NOTIFICATIONS', icon: 'bell', route: 'notifications', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'], group: 'NAV.GROUP_TOOLS' },
    { label: 'NAV.CHANGE_PASSWORD', icon: 'lock', route: 'change-password', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'], group: 'NAV.GROUP_ACCOUNT' },
  ];

  /** Groupes repliés (persistés, tous ouverts par défaut). */
  groupOpen: Record<string, boolean> = LayoutComponent.loadGroupState();
  constructor(
    private loginService: LoginService,
    private router: Router,
    private notificationService: NotificationService,
    private stockService: StockService,
    public themeService: ThemeService,
    private elRef: ElementRef
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

  /** Navigation regroupée en sous-menus (ordre d'apparition conservé). */
  get groupedNav(): NavGroup[] {
    const groups: NavGroup[] = [];
    for (const item of this.filteredNav) {
      let g = groups.find(x => x.label === item.group);
      if (!g) {
        g = { label: item.group, items: [] };
        groups.push(g);
      }
      g.items.push(item);
    }
    return groups;
  }

  isGroupOpen(label: string): boolean {
    return this.groupOpen[label] !== false;
  }

  toggleGroup(label: string): void {
    this.groupOpen[label] = !this.isGroupOpen(label);
    try {
      localStorage.setItem('pp-nav-groups', JSON.stringify(this.groupOpen));
    } catch {
      // stockage indisponible : état en mémoire seulement
    }
  }

  private static loadGroupState(): Record<string, boolean> {
    try {
      const raw = localStorage.getItem('pp-nav-groups');
      if (raw) return JSON.parse(raw);
    } catch {
      // ignore
    }
    return {};
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

  toggleThemePicker(): void {
    this.showThemePicker = !this.showThemePicker;
  }

  closeThemePicker(): void {
    this.showThemePicker = false;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event): void {
    if (!this.showThemePicker) return;
    const target = event.target as HTMLElement;
    if (!this.elRef.nativeElement.querySelector('.theme-picker-wrapper')?.contains(target)) {
      this.showThemePicker = false;
    }
  }

  @HostListener('document:keydown.escape')
  onEscapeKey(): void {
    this.showThemePicker = false;
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
    } else if (notif.relatedEntityType === 'DISPUTE' && notif.relatedEntityId) {
      // Réclamation : détail admin pour SYSTEM_ADMIN, détail boutique pour les
      // boutiques, liste commandes pour les fournisseurs (pas de page dédiée).
      if (this.user?.roles?.includes('SYSTEM_ADMIN')) {
        this.router.navigate(['/dashboard/admin/disputes', notif.relatedEntityId]);
      } else if (this.isShopUser()) {
        this.router.navigate(['/dashboard/shop/disputes', notif.relatedEntityId]);
      } else {
        this.router.navigate(['/dashboard/supplier/orders']);
      }
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
    return timeAgo(dateStr);
  }
}