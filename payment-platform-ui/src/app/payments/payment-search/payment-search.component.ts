import { Component, OnInit } from '@angular/core';
import { PaymentSearchService, SearchPaymentsRequest, PaymentSearchResult } from '../../services/payment-search.service';
import { OrganizationService } from '../../services/organization.service';
import { Organization, SupplierShopRelation } from '../../models/organization.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-payment-search',
  templateUrl: './payment-search.component.html',
  styleUrls: ['./payment-search.component.css']
})
export class PaymentSearchComponent implements OnInit {
  filters: SearchPaymentsRequest = {
    page: 0,
    size: 20
  };
  results: PaymentSearchResult[] = [];
  total = 0;
  loading = false;
  shops: Organization[] = [];
  suppliers: Organization[] = [];
  agents: any[] = [];
  statuses = ['PENDING', 'CONFIRMED', 'REJECTED', 'CANCELLED'];
  user: any;

  constructor(
    private searchService: PaymentSearchService,
    private orgService: OrganizationService,
    private toast: ToastService
  ) {
    const userJson = localStorage.getItem('user');
    if (userJson) {
      this.user = JSON.parse(userJson);
    }
  }

  ngOnInit(): void {
    this.loadOrgs();
    this.search();
  }

  private loadOrgs(): void {
    if (this.user?.roles?.includes('SYSTEM_ADMIN')) {
      this.orgService.listShops().subscribe({ next: (d: Organization[]) => this.shops = d });
      this.orgService.listSuppliers().subscribe({ next: (d: Organization[]) => this.suppliers = d });
    } else if (this.user?.organizationId) {
      const supplierId = this.user.organizationId;
      this.orgService.listRelationsBySupplier(supplierId).subscribe({
        next: (relations: SupplierShopRelation[]) => {
          const shopIds = [...new Set(relations.filter(r => r.status === 'ACTIVE').map(r => r.shopId))];
          shopIds.forEach(id => {
            this.orgService.getById(id).subscribe({
              next: (shop) => this.shops.push(shop)
            });
          });
        }
      });
    }
  }

  search(): void {
    this.loading = true;
    this.searchService.search(this.filters).subscribe({
      next: (res) => {
        this.results = res.payments;
        this.total = res.total;
        this.loading = false;
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur de recherche');
        this.loading = false;
      }
    });
  }

  resetFilters(): void {
    this.filters = { page: 0, size: 20 };
    this.search();
  }

  nextPage(): void {
    this.filters.page++;
    this.search();
  }

  prevPage(): void {
    if (this.filters.page > 0) {
      this.filters.page--;
      this.search();
    }
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = {
      PENDING: 'En attente', CONFIRMED: 'Confirmé',
      REJECTED: 'Rejeté', CANCELLED: 'Annulé'
    };
    return map[s] || s;
  }

  get totalPages(): number {
    return Math.ceil(this.total / this.filters.size);
  }
}
