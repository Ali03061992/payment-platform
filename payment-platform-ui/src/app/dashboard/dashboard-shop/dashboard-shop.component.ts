import { Component, OnInit } from '@angular/core';
import { LoginService } from '../../services/login.service';
import { OrderService } from '../../services/order.service';
import { PaymentService } from '../../services/payment.service';
import { BalanceService } from '../../services/balance.service';
import { QuoteService } from '../../services/quote.service';
import { User } from '../../models/user.model';
import { Order } from '../../models/order.model';
import { Payment, PaymentStats } from '../../models/payment.model';
import { BalanceSummary } from '../../models/balance.model';
import { statusLabelFr } from '../../pipes/status-label.pipe';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'app-dashboard-shop',
    templateUrl: './dashboard-shop.component.html',
    styleUrls: ['./dashboard-shop.component.css'],
    standalone: false
})
export class DashboardShopComponent implements OnInit {
  user: User | null = null;
  recentOrders: Order[] = [];
  recentPayments: Payment[] = [];
  paymentStats: PaymentStats = { total: 0, pending: 0, confirmed: 0, rejected: 0, cancelled: 0 };
  totalOrders = 0;
  pendingOrdersCount = 0;
  deliveredOrdersCount = 0;
  inTransitCount = 0;
  pendingQuotesCount = 0;
  supplierBalances: BalanceSummary[] = [];
  loading = true;
  reorderLoading: Record<string, boolean> = {};

  constructor(
    private loginService: LoginService,
    private orderService: OrderService,
    private paymentService: PaymentService,
    private balanceService: BalanceService,
    private quoteService: QuoteService,
    private translate: TranslateService
  ) {}

  /** Encours total dû aux fournisseurs (Σ remainingDue). */
  get totalDueToSuppliers(): number {
    return this.supplierBalances.reduce((sum, b) => sum + Number(b.remainingDue || 0), 0);
  }

  /** Nombre de fournisseurs avec un solde actif. */
  get supplierCount(): number {
    return this.supplierBalances.length;
  }

  ngOnInit(): void {
    this.user = this.loginService.getCurrentUser();
    this.loadStats();
  }

  isShopAdmin(): boolean {
    return this.loginService.hasRole('SHOP_ADMIN');
  }

  loadStats(): void {
    this.loading = true;
    let completed = 0;
    const total = 6;

    const checkDone = () => {
      completed++;
      if (completed >= total) this.loading = false;
    };

    this.orderService.getRecent(5).subscribe({
      next: (orders) => {
        this.recentOrders = orders;
        checkDone();
      },
      error: () => checkDone()
    });

    this.orderService.listPaged(0, 100).subscribe({
      next: (page) => {
        try {
          // totalElements est le vrai total serveur (items est plafonné à 100).
          this.totalOrders = page.totalElements;
          const orders = page.items;
          // Statuts réels du cycle de commande (PENDING/READY/ASSIGNED n'existent pas).
          this.pendingOrdersCount = orders.filter(o => ['DRAFT', 'CONFIRMED', 'PREPARING'].includes(o.status)).length;
          this.deliveredOrdersCount = orders.filter(o => ['DELIVERED', 'ACCEPTED'].includes(o.status)).length;
          this.inTransitCount = orders.filter(o => ['READY_FOR_DELIVERY', 'DELIVERY_ACCEPTED', 'IN_DELIVERY'].includes(o.status)).length;
        } finally {
          checkDone();
        }
      },
      error: () => checkDone()
    });

    this.paymentService.list().subscribe({
      next: (payments) => {
        this.recentPayments = payments.slice(0, 5);
        checkDone();
      },
      error: () => checkDone()
    });

    this.paymentService.getStats().subscribe({
      next: (s) => { this.paymentStats = s; checkDone(); },
      error: () => checkDone()
    });

    const shopId = this.loginService.getCurrentUser()?.organizationId;
    if (shopId) {
      this.balanceService.listByShop(String(shopId)).subscribe({
        next: (balances) => { this.supplierBalances = balances; checkDone(); },
        error: () => checkDone()
      });
    } else {
      checkDone();
    }

    this.quoteService.myRequests().subscribe({
      next: (requests) => {
        // QUOTED = fournisseur a répondu, en attente de décision de la boutique.
        this.pendingQuotesCount = requests.filter(q => q.status === 'QUOTED').length;
        checkDone();
      },
      error: () => checkDone()
    });
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

  getPaymentStatusClass(status: string): string {
    const classes: Record<string, string> = {
      'PENDING': 'badge-warning',
      'CONFIRMED': 'badge-success',
      'REJECTED': 'badge-danger',
      'CANCELLED': 'badge-danger'
    };
    return classes[status] || '';
  }

  getPaymentStatusLabel(status: string): string {
    const keys: Record<string, string> = {
      'PENDING': 'PAYMENTS.PENDING',
      'CONFIRMED': 'PAYMENTS.CONFIRMED',
      'REJECTED': 'PAYMENTS.REJECTED',
      'CANCELLED': 'PAYMENTS.CANCELLED'
    };
    const key = keys[status];
    return key ? this.translate.instant(key) : status;
  }

  reorder(order: Order): void {
    this.reorderLoading[order.id] = true;
    this.orderService.reorder(order.id).subscribe({
      next: () => {
        this.reorderLoading[order.id] = false;
        this.loadStats();
      },
      error: () => {
        this.reorderLoading[order.id] = false;
      }
    });
  }

  canReorder(order: Order): boolean {
    return ['DELIVERED', 'ACCEPTED', 'CANCELLED', 'REJECTED'].includes(order.status);
  }
}