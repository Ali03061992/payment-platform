import { Component, OnInit } from '@angular/core';
import { LoginService } from '../services/login.service';
import { UserService } from '../services/user.service';
import { User } from '../models/user.model';

@Component({
    selector: 'app-dashboard',
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.css'],
    standalone: false
})
export class DashboardComponent implements OnInit {
  user: User | null = null;
  stats = { totalUsers: 0, activeUsers: 0, disabledUsers: 0, suppliers: 0, shops: 0 };

  constructor(private loginService: LoginService, private userService: UserService) {}

  ngOnInit(): void {
    this.user = this.loginService.getCurrentUser();
    if (this.loginService.hasRole('SYSTEM_ADMIN')) {
      this.userService.list().subscribe(users => {
        this.stats.totalUsers = users.length;
        this.stats.activeUsers = users.filter(u => u.status === 'ACTIVE').length;
        this.stats.disabledUsers = users.filter(u => u.status === 'DISABLED').length;
        this.stats.suppliers = users.filter(u => u.roles.some(r => r.startsWith('SUPPLIER'))).length;
        this.stats.shops = users.filter(u => u.roles.some(r => r.startsWith('SHOP'))).length;
      });
    }
  }

  getGreeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Bonjour';
    if (hour < 18) return 'Bon après-midi';
    return 'Bonsoir';
  }
}
