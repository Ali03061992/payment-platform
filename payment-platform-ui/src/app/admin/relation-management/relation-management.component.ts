import { Component, OnInit } from '@angular/core';
import { OrganizationService } from '../../services/organization.service';
import { Organization, SupplierShopRelation } from '../../models/organization.model';
import { paginateItems } from '../../models/page.model';
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
  pageSize = 20;

  constructor(private orgService: OrganizationService, private toast: ToastService) {}

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
    return paginateItems(this.relations, this.currentPage, this.pageSize);
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
        this.toast.success('Relation créée avec succès');
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
