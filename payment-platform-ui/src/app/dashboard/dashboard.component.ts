import { Component, OnInit } from '@angular/core';
import { LoginService } from '../services/login.service';
import { User } from '../models/user.model';
import { TranslateService } from '@ngx-translate/core';
import { Title } from '@angular/platform-browser';

@Component({
    selector: 'app-dashboard',
    templateUrl: './dashboard.component.html',
    styleUrls: ['./dashboard.component.css'],
    standalone: false
})
export class DashboardComponent implements OnInit {
  user: User | null = null;
  activeRole: string | null = null;

  constructor(private loginService: LoginService, private translate: TranslateService, private title: Title) {}

  ngOnInit(): void {
    this.title.setTitle('NEXTERA - Tableau de bord');
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
    return 'Bienvenue';
  }
}