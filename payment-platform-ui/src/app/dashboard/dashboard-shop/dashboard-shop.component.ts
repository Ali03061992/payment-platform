import { Component, OnInit } from '@angular/core';
import { LoginService } from '../../services/login.service';
import { OrderService } from '../../services/order.service';
import { PaymentService } from '../../services/payment.service';
import { User } from '../../models/user.model';
import { Order } from '../../models/order.model';
import { Payment, PaymentStats } from '../../models/payment.model';
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
  loading = true;
  reorderLoading: Record<string, boolean> = {};

  constructor(
    private loginService: LoginService,
    private orderService: OrderService,
    private paymentService: PaymentService,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.user = this.loginService.getCurrentUser();
    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;
    let completed = 0;
    const total = 4;

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

    this.orderService.list().subscribe({
      next: (orders) => {
        try {
          this.totalOrders = orders.length;
          this.pendingOrdersCount = orders.filter(o => ['PENDING', 'CONFIRMED', 'PREPARING'].includes(o.status)).length;
          this.deliveredOrdersCount = orders.filter(o => o.status === 'DELIVERED').length;
          this.inTransitCount = orders.filter(o => ['READY', 'ASSIGNED'].includes(o.status)).length;
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
      'PENDING': 'badge-warning',
      'CONFIRMED': 'badge-info',
      'PREPARING': 'badge-info',
      'READY': 'badge-info',
      'ASSIGNED': 'badge-info',
      'DELIVERED': 'badge-success',
      'CANCELLED': 'badge-danger'
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