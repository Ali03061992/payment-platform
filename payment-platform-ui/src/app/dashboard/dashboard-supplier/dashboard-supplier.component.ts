import { Component, OnInit } from '@angular/core';
import { LoginService } from '../../services/login.service';
import { StockService } from '../../services/stock.service';
import { OrderService } from '../../services/order.service';
import { PaymentService } from '../../services/payment.service';
import { User } from '../../models/user.model';
import { Product } from '../../models/stock.model';
import { Order } from '../../models/order.model';
import { PaymentStats } from '../../models/payment.model';

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
  loading = true;

  constructor(
    private loginService: LoginService,
    private stockService: StockService,
    private orderService: OrderService,
    private paymentService: PaymentService
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

    this.orderService.list().subscribe({
      next: (orders) => {
        try {
          this.recentOrders = orders.slice(0, 5);
          this.pendingOrdersCount = orders.filter(o => ['PENDING', 'CONFIRMED', 'PREPARING'].includes(o.status)).length;
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
  }

  getGreeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Bonjour';
    if (hour < 18) return 'Bon après-midi';
    return 'Bonsoir';
  }

  getStatusLabel(status: string): string {
    const labels: Record<string, string> = {
      'PENDING': 'En attente',
      'CONFIRMED': 'Confirmée',
      'PREPARING': 'En préparation',
      'READY': 'Prête',
      'DELIVERED': 'Livrée',
      'CANCELLED': 'Annulée'
    };
    return labels[status] || status;
  }

  getStatusClass(status: string): string {
    const classes: Record<string, string> = {
      'PENDING': 'badge-warning',
      'CONFIRMED': 'badge-info',
      'PREPARING': 'badge-info',
      'READY': 'badge-success',
      'DELIVERED': 'badge-success',
      'CANCELLED': 'badge-danger'
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
