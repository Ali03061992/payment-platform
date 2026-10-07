import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { OrganizationService } from '../../services/organization.service';
import { Organization, SupplierShopRelation } from '../../models/organization.model';
import { paginateItems, sortItems, toggleSortState, ariaSortFor, sortIndicatorFor, SortState } from '../../models/page.model';
import { ToastService } from '../../services/toast.service';

@Component({
    selector: 'app-relation-management',
    templateUrl: './relation-management.component.html',
    styleUrls: ['./relation-management.component.css'],
    standalone: false
})
export class RelationManagementComponent implements OnInit {
  relations: SupplierShopRelation[] = [];
  suppliers: Organization[] = [];
  shops: Organization[] = [];
  loading = true;
  showCreate = false;
  selectedSupplierId = '';
  selectedShopId = '';
  creating = false;
  currentPage = 0;
  pageSize = 10;
  sort: SortState = { field: null, direction: 'asc' };

  constructor(private orgService: OrganizationService, private toast: ToastService, private translate: TranslateService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.orgService.listRelations().subscribe({
      next: (data: SupplierShopRelation[]) => { this.relations = data; this.currentPage = 0; this.loading = false; }
    });
    this.orgService.listSuppliers().subscribe({ next: (d: Organization[]) => this.suppliers = d });
    this.orgService.listShops().subscribe({ next: (d: Organization[]) => this.shops = d });
  }

  get pagedRelations(): SupplierShopRelation[] {
    return paginateItems(this.sortedRelations, this.currentPage, this.pageSize);
  }

  get sortedRelations(): SupplierShopRelation[] {
    return sortItems(this.relations, this.sort.field, this.sort.direction);
  }

  onSort(field: string): void {
    this.sort = toggleSortState(this.sort, field);
    this.currentPage = 0;
  }

  ariaSort(field: string): 'ascending' | 'descending' | 'none' {
    return ariaSortFor(field, this.sort);
  }

  sortIndicator(field: string): string {
    return sortIndicatorFor(field, this.sort);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
  }

  onSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
  }

  create(): void {
    if (!this.selectedSupplierId || !this.selectedShopId) return;
    this.creating = true;
    this.orgService.createRelation({ supplierId: this.selectedSupplierId, shopId: this.selectedShopId }).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('ORGS.RELATION_CREATED'));
        this.showCreate = false;
        this.creating = false;
        this.load();
      },
      error: (err: { error?: { message?: string }; status?: number; statusText?: string; message?: string }) => {
        this.toast.error(err.error?.message || this.translate.instant('ORGS.CREATE_ERROR'));
        this.creating = false;
      }
    });
  }

  deactivate(id: string): void {
    this.orgService.deactivateRelation(id).subscribe({ next: () => this.load() });
  }

  getSupplierName(id: string): string {
    return this.suppliers.find(s => s.id === id)?.name || `#${id}`;
  }

  getShopName(id: string): string {
    return this.shops.find(s => s.id === id)?.name || `#${id}`;
  }
}
