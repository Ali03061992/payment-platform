import { Component, OnInit } from '@angular/core';
import { LoginService } from '../../services/login.service';
import { OrderService } from '../../services/order.service';
import { User } from '../../models/user.model';
import { Order } from '../../models/order.model';
import { statusLabelFr } from '../../pipes/status-label.pipe';
import { TranslateService } from '@ngx-translate/core';

@Component({
    selector: 'app-dashboard-agent',
    templateUrl: './dashboard-agent.component.html',
    styleUrls: ['./dashboard-agent.component.css'],
    standalone: false
})
export class DashboardAgentComponent implements OnInit {
  user: User | null = null;
  assignedOrders: Order[] = [];
  inProgressOrders: Order[] = [];
  completedToday: Order[] = [];
  assignedCount = 0;
  inProgressCount = 0;
  completedTodayCount = 0;
  loading = true;

  constructor(
    private loginService: LoginService,
    private orderService: OrderService,
    private translate: TranslateService
  ) {}

  ngOnInit(): void {
    this.user = this.loginService.getCurrentUser();
    this.loadStats();
  }

  loadStats(): void {
    this.loading = true;
    this.orderService.myDeliveries().subscribe({
      next: (orders) => {
        const today = new Date().toISOString().split('T')[0];
        // Statuts réels du cycle de livraison (OrderStatus) :
        // assignée = prête pour livraison, en cours = acceptée/parcours, livrée = DELIVERED.
        const assigned = orders.filter(o => o.status === 'READY_FOR_DELIVERY');
        const inProgress = orders.filter(o => ['DELIVERY_ACCEPTED', 'IN_DELIVERY'].includes(o.status));
        const doneToday = orders.filter(o =>
          o.status === 'DELIVERED' && o.deliveredAt && o.deliveredAt.startsWith(today)
        );

        this.assignedOrders = assigned.slice(0, 5);
        this.inProgressOrders = inProgress.slice(0, 5);
        this.completedToday = doneToday.slice(0, 5);

        this.assignedCount = assigned.length;
        this.inProgressCount = inProgress.length;
        this.completedTodayCount = doneToday.length;

        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
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
      'READY_FOR_DELIVERY': 'badge-warning',
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
}