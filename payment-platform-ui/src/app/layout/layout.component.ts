import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { LoginService } from '../services/login.service';

@Component({
  selector: 'app-layout',
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.css']
})
export class LayoutComponent {
  user: any;
  sidebarOpen = true;

  navItems: { label: string; icon: string; route: string; roles: string[] }[] = [
    { label: 'Tableau de bord', icon: '📊', route: '/dashboard', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT', 'SALES'] },
    { label: 'Gestion des utilisateurs', icon: '👥', route: '/admin/users', roles: ['SYSTEM_ADMIN'] },
    { label: 'Créer un compte', icon: '➕', route: '/admin/users/create', roles: ['SYSTEM_ADMIN'] },
    { label: 'Activation comptes', icon: '🔑', route: '/sales/accounts', roles: ['SALES', 'SYSTEM_ADMIN'] },
    { label: 'Gestion du stock', icon: '📦', route: '/supplier/stock', roles: ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'] },
    { label: 'Ajouter un produit', icon: '➕', route: '/supplier/stock/create', roles: ['SUPPLIER_ADMIN'] },
  ];

  constructor(private loginService: LoginService, private router: Router) {
    this.user = this.loginService.getCurrentUser();
  }

  get filteredNav() {
    return this.navItems.filter(item => item.roles.some(r => this.user?.roles?.includes(r)));
  }

  toggleSidebar(): void {
    this.sidebarOpen = !this.sidebarOpen;
  }

  logout(): void {
    this.loginService.logout();
    this.router.navigate(['/login']);
  }

  getInitials(): string {
    if (!this.user) return '?';
    return (this.user.firstName?.[0] || '') + (this.user.lastName?.[0] || '');
  }
}
