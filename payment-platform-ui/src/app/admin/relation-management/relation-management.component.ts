import { Component, OnInit } from '@angular/core';
import { OrganizationService } from '../../services/organization.service';
import { Organization, SupplierShopRelation } from '../../models/organization.model';

@Component({
  selector: 'app-relation-management',
  templateUrl: './relation-management.component.html',
  styleUrls: ['./relation-management.component.css']
})
export class RelationManagementComponent implements OnInit {
  relations: SupplierShopRelation[] = [];
  suppliers: Organization[] = [];
  shops: Organization[] = [];
  loading = true;
  showCreate = false;
  selectedSupplierId = 0;
  selectedShopId = 0;
  creating = false;
  successMsg = '';
  errorMsg = '';

  constructor(private orgService: OrganizationService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.orgService.listRelations().subscribe({
      next: (data: SupplierShopRelation[]) => { this.relations = data; this.loading = false; }
    });
    this.orgService.listSuppliers().subscribe({ next: (d: Organization[]) => this.suppliers = d });
    this.orgService.listShops().subscribe({ next: (d: Organization[]) => this.shops = d });
  }

  create(): void {
    if (!this.selectedSupplierId || !this.selectedShopId) return;
    this.creating = true;
    this.orgService.createRelation({ supplierId: this.selectedSupplierId, shopId: this.selectedShopId }).subscribe({
      next: () => {
        this.successMsg = 'Relation créée avec succès';
        this.showCreate = false;
        this.creating = false;
        this.load();
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: (err: any) => {
        this.errorMsg = err.error?.message || 'Erreur lors de la création';
        this.creating = false;
        setTimeout(() => this.errorMsg = '', 3000);
      }
    });
  }

  deactivate(id: number): void {
    this.orgService.deactivateRelation(id).subscribe({ next: () => this.load() });
  }

  getSupplierName(id: number): string {
    return this.suppliers.find(s => s.id === id)?.name || `#${id}`;
  }

  getShopName(id: number): string {
    return this.shops.find(s => s.id === id)?.name || `#${id}`;
  }
}
