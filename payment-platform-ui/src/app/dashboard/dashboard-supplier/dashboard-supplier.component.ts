import { Component, OnInit } from '@angular/core';
import { LoginService } from '../../services/login.service';
import { StockService } from '../../services/stock.service';
import { OrderService } from '../../services/order.service';
import { PaymentService } from '../../services/payment.service';
import { BalanceService } from '../../services/balance.service';
import { User } from '../../models/user.model';
import { Product } from '../../models/stock.model';
import { Order } from '../../models/order.model';
import { PaymentStats } from '../../models/payment.model';
import { BalanceSummary } from '../../models/balance.model';
import { statusLabelFr } from '../../pipes/status-label.pipe';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'app-dashboard-supplier',
    templateUrl: './dashboard-supplier.component.html',
    styleUrls: ['./dashboard-supplier.component.css'],
    standalone: false
})
export class DashboardSupplierComponent implements OnInit {
  user: User | null = null;
  products: Product[] = [];
  lowStockAlerts: Product[] = [];
  recentOrders: Order[] = [];
  paymentStats: PaymentStats = { total: 0, pending: 0, confirmed: 0, rejected: 0, cancelled: 0 };
  lowStockCount = 0;
  outOfStockCount = 0;
  pendingOrdersCount = 0;
  totalOrders = 0;
  shopBalances: BalanceSummary[] = [];
  loading = true;

  constructor(
    private loginService: LoginService,
    private stockService: StockService,
    private orderService: OrderService,
    private paymentService: PaymentService,
    private balanceService: BalanceService,
    private translate: TranslateService
  ) {}

  /** Encours total dû par les boutiques (Σ remainingDue). */
  get totalDueByShops(): number {
    return this.shopBalances.reduce((sum, b) => sum + Number(b.remainingDue || 0), 0);
  }

  /** Nombre de boutiques actives (avec un solde). */
  get activeShopCount(): number {
    return this.shopBalances.length;
  }

  ngOnInit(): void {
    this.user = this.loginService.getCurrentUser();
    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;
    let completed = 0;
    const total = 5;

    const checkDone = () => {
      completed++;
      if (completed >= total) this.loading = false;
    };

    this.stockService.getProducts().subscribe({
      next: (products) => {
        try {
          this.products = products;
          this.lowStockCount = products.filter(p => p.quantity <= p.minQuantity && p.quantity > 0).length;
          this.outOfStockCount = products.filter(p => p.quantity === 0).length;
        } finally {
          checkDone();
        }
      },
      error: () => checkDone()
    });

    this.stockService.getLowStockAlerts().subscribe({
      next: (alerts) => { this.lowStockAlerts = alerts.slice(0, 5); checkDone(); },
      error: () => checkDone()
    });

    this.orderService.listPaged(0, 100).subscribe({
      next: (page) => {
        try {
          this.recentOrders = page.items.slice(0, 5);
          // totalElements est le vrai total serveur (items est plafonné à 100).
          this.totalOrders = page.totalElements;
          // Statuts réels du cycle de commande (PENDING n'existe pas, c'est DRAFT).
          this.pendingOrdersCount = page.items.filter(o => ['DRAFT', 'CONFIRMED', 'PREPARING'].includes(o.status)).length;
        } finally {
          checkDone();
        }
      },
      error: () => checkDone()
    });

    this.paymentService.getStats().subscribe({
      next: (s) => { this.paymentStats = s; checkDone(); },
      error: () => checkDone()
    });

    const supplierId = this.loginService.getCurrentUser()?.organizationId;
    if (supplierId) {
      this.balanceService.listBySupplier(String(supplierId)).subscribe({
        next: (balances) => { this.shopBalances = balances; checkDone(); },
        error: () => checkDone()
      });
    } else {
      checkDone();
    }
  }

  getGreeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return this.translate.instant('DASHBOARD.MORNING');
    if (hour < 18) return this.translate.instant('DASHBOARD.AFTERNOON');
    return this.translate.instant('DASHBOARD.EVENING');
  }

  getStatusLabel(status: string): string {
    return statusLabelFr(status);
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = {
      'DRAFT': 'badge-warning',
      'CONFIRMED': 'badge-info',
      'PREPARING': 'badge-info',
      'READY_FOR_DELIVERY': 'badge-info',
      'DELIVERY_ACCEPTED': 'badge-info',
      'IN_DELIVERY': 'badge-info',
      'DELIVERY_REJECTED': 'badge-danger',
      'DELIVERED': 'badge-success',
      'ACCEPTED': 'badge-success',
      'CANCELLED': 'badge-danger',
      'REJECTED': 'badge-danger'
    };
    return classes[status] || '';
  }

  getAvailableQty(product: Product): number {
    return product.quantity - product.reservedQty;
  }

  getUrgencyLevel(product: Product): string {
    const available = product.quantity - product.reservedQty;
    if (available <= 0) return 'critical';
    if (available <= Math.floor(product.minQuantity / 2)) return 'high';
    return 'medium';
  }
}