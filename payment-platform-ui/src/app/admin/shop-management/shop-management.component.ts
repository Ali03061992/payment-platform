import { Component, OnInit } from '@angular/core';
import { OrganizationService } from '../../services/organization.service';
import { Organization } from '../../models/organization.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-shop-management',
  templateUrl: './shop-management.component.html',
  styleUrls: ['./shop-management.component.css']
})
export class ShopManagementComponent implements OnInit {
  shops: Organization[] = [];
  loading = true;
  showCreate = false;
  newName = '';
  creating = false;
  constructor(private orgService: OrganizationService, private toast: ToastService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.orgService.listShops().subscribe({
      next: (data: Organization[]) => { this.shops = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  create(): void {
    if (!this.newName.trim()) return;
    this.creating = true;
    this.orgService.createShop(this.newName.trim()).subscribe({
      next: () => {
        this.toast.success('Boutique créée avec succès');
        this.newName = '';
        this.showCreate = false;
        this.creating = false;
        this.load();
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur lors de la création');
        this.creating = false;
      }
    });
  }

  toggle(shop: Organization): void {
    const obs = shop.status === 'ACTIVE'
      ? this.orgService.disable(shop.id, 'SHOP')
      : this.orgService.activate(shop.id, 'SHOP');
    obs.subscribe({ next: () => this.load() });
  }
}
