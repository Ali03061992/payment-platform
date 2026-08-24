import { Component, OnInit } from '@angular/core';
import { OrganizationService } from '../../services/organization.service';
import { Organization } from '../../models/organization.model';

@Component({
  selector: 'app-supplier-management',
  templateUrl: './supplier-management.component.html',
  styleUrls: ['./supplier-management.component.css']
})
export class SupplierManagementComponent implements OnInit {
  suppliers: Organization[] = [];
  loading = true;
  showCreate = false;
  newName = '';
  creating = false;
  successMsg = '';
  errorMsg = '';

  constructor(private orgService: OrganizationService) {}

  ngOnInit(): void { this.load(); }

  load(): void {
    this.loading = true;
    this.orgService.listSuppliers().subscribe({
      next: (data: Organization[]) => { this.suppliers = data; this.loading = false; },
      error: () => { this.loading = false; }
    });
  }

  create(): void {
    if (!this.newName.trim()) return;
    this.creating = true;
    this.orgService.createSupplier(this.newName.trim()).subscribe({
      next: () => {
        this.successMsg = 'Fournisseur créé avec succès';
        this.newName = '';
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

  toggle(supplier: Organization): void {
    const obs = supplier.status === 'ACTIVE'
      ? this.orgService.disable(supplier.id, 'SUPPLIER')
      : this.orgService.activate(supplier.id, 'SUPPLIER');
    obs.subscribe({
      next: () => { this.load(); },
      error: () => {}
    });
  }
}
