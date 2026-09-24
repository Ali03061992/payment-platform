import { Component, OnInit } from '@angular/core';
import { LoginService } from '../../services/login.service';
import { UserService } from '../../services/user.service';
import { OrganizationService } from '../../services/organization.service';
import { PaymentService } from '../../services/payment.service';
import { OrderService } from '../../services/order.service';
import { User } from '../../models/user.model';
import { OrganizationStats } from '../../models/organization.model';
import { PaymentStats } from '../../models/payment.model';

@Component({
    selector: 'app-dashboard-admin',
    templateUrl: './dashboard-admin.component.html',
    styleUrls: ['./dashboard-admin.component.css'],
    standalone: false
})
export class DashboardAdminComponent implements OnInit {
  user: User | null = null;
  stats = { totalUsers: 0, activeUsers: 0, disabledUsers: 0, suppliers: 0, shops: 0 };
  orgStats: OrganizationStats = { suppliers: 0, shops: 0 };
  paymentStats: PaymentStats = { total: 0, pending: 0, confirmed: 0, rejected: 0, cancelled: 0 };
  recentOrdersCount = 0;
  loading = true;

  constructor(
    private loginService: LoginService,
    private userService: UserService,
    private organizationService: OrganizationService,
    private paymentService: PaymentService,
    private orderService: OrderService
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

    this.userService.list().subscribe({
      next: (users) => {
        try {
          this.stats.totalUsers = users.length;
          this.stats.activeUsers = users.filter(u => u.status === 'ACTIVE').length;
          this.stats.disabledUsers = users.filter(u => u.status === 'DISABLED').length;
          this.stats.suppliers = users.filter(u => u.roles?.some(r => r.startsWith('SUPPLIER'))).length;
          this.stats.shops = users.filter(u => u.roles?.some(r => r.startsWith('SHOP'))).length;
        } finally {
          checkDone();
        }
      },
      error: () => checkDone()
    });

    this.organizationService.getStats().subscribe({
      next: (s) => { this.orgStats = s; checkDone(); },
      error: () => checkDone()
    });

    this.paymentService.getStats().subscribe({
      next: (s) => { this.paymentStats = s; checkDone(); },
      error: () => checkDone()
    });

    this.orderService.list().subscribe({
      next: (orders) => {
        try {
          this.recentOrdersCount = orders.length;
        } finally {
          checkDone();
        }
      },
      error: () => checkDone()
    });
  }

  getGreeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Bonjour';
    if (hour < 18) return 'Bon après-midi';
    return 'Bonsoir';
  }
}
