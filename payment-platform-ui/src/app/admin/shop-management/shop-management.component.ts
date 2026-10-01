import { Component, OnInit } from '@angular/core';
import { OrganizationService } from '../../services/organization.service';
import { Organization } from '../../models/organization.model';
import { PageResponse } from '../../models/page.model';
import { ToastService } from '../../services/toast.service';

@Component({
    selector: 'app-shop-management',
    templateUrl: './shop-management.component.html',
    styleUrls: ['./shop-management.component.css'],
    standalone: false
})
export class ShopManagementComponent implements OnInit {
  shops: Organization[] = [];
  loading = true;
  showCreate = false;
  newName = '';
  creating = false;
  currentPage = 0;
  pageSize = 20;
  totalElements = 0;
  totalPages = 0;
  constructor(private orgService: OrganizationService, private toast: ToastService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.orgService.listShopsPaged(this.currentPage, this.pageSize).subscribe({
      next: (page: PageResponse<Organization>) => {
        this.shops = page.items || [];
        this.totalElements = page.totalElements ?? this.shops.length;
        this.totalPages = page.totalPages ?? 1;
        this.currentPage = page.number ?? this.currentPage;
        this.loading = false;
      },
      error: () => { this.loading = false; }
    });
  }

  onPageChange(page: number): void {
    this.currentPage = page;
    this.load();
  }

  onSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
    this.load();
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
