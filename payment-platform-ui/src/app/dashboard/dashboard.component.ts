import { Component, OnInit } from '@angular/core';
import { LoginService } from '../services/login.service';
import { User } from '../models/user.model';

@Component({
    selector: 'app-dashboard',
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.css'],
    standalone: false
})
export class DashboardComponent implements OnInit {
  user: User | null = null;
  activeRole: string | null = null;

  constructor(private loginService: LoginService) {}

  ngOnInit(): void {
    this.user = this.loginService.getCurrentUser();
    this.activeRole = this.detectRole();
  }

  private detectRole(): string | null {
    if (!this.user?.roles?.length) return null;
    if (this.user.roles.includes('SYSTEM_ADMIN')) return 'SYSTEM_ADMIN';
    if (this.user.roles.includes('SUPPLIER_AGENT')) return 'SUPPLIER_AGENT';
    if (this.user.roles.includes('SUPPLIER_ADMIN')) return 'SUPPLIER_ADMIN';
    if (this.user.roles.includes('SHOP_ADMIN')) return 'SHOP_ADMIN';
    if (this.user.roles.includes('SHOP_AGENT')) return 'SHOP_AGENT';
    return null;
  }

  getGreeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Bonjour';
    if (hour < 18) return 'Bon après-midi';
    return 'Bonsoir';
  }
}
