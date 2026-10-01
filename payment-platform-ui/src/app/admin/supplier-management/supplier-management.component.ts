import { Component, OnInit } from '@angular/core';
import { OrganizationService } from '../../services/organization.service';
import { Organization } from '../../models/organization.model';
import { PageResponse } from '../../models/page.model';
import { ToastService } from '../../services/toast.service';

@Component({
    selector: 'app-supplier-management',
    templateUrl: './supplier-management.component.html',
    styleUrls: ['./supplier-management.component.css'],
    standalone: false
})
export class SupplierManagementComponent implements OnInit {
  suppliers: Organization[] = [];
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
    this.orgService.listSuppliersPaged(this.currentPage, this.pageSize).subscribe({
      next: (page: PageResponse<Organization>) => {
        this.suppliers = page.items || [];
        this.totalElements = page.totalElements ?? this.suppliers.length;
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
    this.orgService.createSupplier(this.newName.trim()).subscribe({
      next: () => {
        this.toast.success('Fournisseur créé avec succès');
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
