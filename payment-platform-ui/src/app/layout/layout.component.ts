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
    // System Admin
    { label: 'Tableau de bord', icon: '📊', route: '', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'] },
    { label: 'Gestion des utilisateurs', icon: '👥', route: 'admin/users', roles: ['SYSTEM_ADMIN'] },
    { label: 'Créer un compte', icon: '➕', route: 'admin/users/create', roles: ['SYSTEM_ADMIN'] },
    { label: 'Activation comptes', icon: '🔑', route: 'sales/accounts', roles: ['SYSTEM_ADMIN'] },
    { label: 'Fournisseurs', icon: '🏭', route: 'admin/suppliers', roles: ['SYSTEM_ADMIN'] },
    { label: 'Boutiques', icon: '🏪', route: 'admin/shops', roles: ['SYSTEM_ADMIN'] },
    { label: 'Relations F-B', icon: '🔗', route: 'admin/relations', roles: ['SYSTEM_ADMIN'] },
    { label: 'Stats organisations', icon: '📈', route: 'admin/org-stats', roles: ['SYSTEM_ADMIN'] },
    // Supplier
    { label: 'Catalogue', icon: '📋', route: 'supplier/products', roles: ['SUPPLIER_ADMIN'] },
    { label: 'Stock', icon: '📦', route: 'supplier/stock', roles: ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'] },
    { label: 'Commandes', icon: '🛒', route: 'supplier/orders', roles: ['SUPPLIER_ADMIN'] },
    { label: 'Livraisons', icon: '🚚', route: 'supplier/deliveries', roles: ['SUPPLIER_ADMIN', 'SUPPLIER_AGENT'] },
    // Shop
    { label: 'Mes commandes', icon: '🛒', route: 'shop/orders', roles: ['SHOP_ADMIN', 'SHOP_MANAGER', 'SHOP_AGENT'] },
    { label: 'Nouvelle commande', icon: '➕', route: 'shop/orders/create', roles: ['SHOP_ADMIN', 'SHOP_MANAGER'] },
    { label: 'Balance', icon: '💰', route: 'shop/balance', roles: ['SHOP_ADMIN', 'SHOP_MANAGER'] },
    // Common
    { label: 'Paiements', icon: '💰', route: 'payments', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'] },
    { label: 'Stats paiements', icon: '📊', route: 'payments/stats', roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN', 'SUPPLIER_AGENT', 'SHOP_ADMIN', 'SHOP_AGENT'] },
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
