import { Component, OnInit } from '@angular/core';
import { LoginService } from '../../services/login.service';
import { OrderService } from '../../services/order.service';
import { User } from '../../models/user.model';
import { Order } from '../../models/order.model';

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
    private orderService: OrderService
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

        this.assignedOrders = orders.filter(o => o.status === 'ASSIGNED').slice(0, 5);
        this.inProgressOrders = orders.filter(o => ['READY', 'CONFIRMED'].includes(o.status)).slice(0, 5);
        this.completedToday = orders.filter(o =>
          o.status === 'DELIVERED' && o.deliveredAt && o.deliveredAt.startsWith(today)
        ).slice(0, 5);

        this.assignedCount = orders.filter(o => o.status === 'ASSIGNED').length;
        this.inProgressCount = orders.filter(o => ['READY', 'CONFIRMED'].includes(o.status)).length;
        this.completedTodayCount = orders.filter(o =>
          o.status === 'DELIVERED' && o.deliveredAt && o.deliveredAt.startsWith(today)
        ).length;

        this.loading = false;
      },
      error: () => {
        this.loading = false;
      }
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
      'ASSIGNED': 'Assignée',
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
      'READY': 'badge-info',
      'ASSIGNED': 'badge-warning',
      'DELIVERED': 'badge-success',
      'CANCELLED': 'badge-danger'
    };
    return classes[status] || '';
  }
}
