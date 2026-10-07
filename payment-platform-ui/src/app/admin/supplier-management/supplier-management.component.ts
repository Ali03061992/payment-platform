import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { OrganizationService } from '../../services/organization.service';
import { Organization } from '../../models/organization.model';
import { PageResponse, sortItems, toggleSortState, ariaSortFor, sortIndicatorFor, SortState } from '../../models/page.model';
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
  pageSize = 10;
  totalElements = 0;
  totalPages = 0;
  sort: SortState = { field: null, direction: 'asc' };

  constructor(private orgService: OrganizationService, private toast: ToastService, private translate: TranslateService) {}

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

  onSort(field: string): void {
    this.sort = toggleSortState(this.sort, field);
  }

  ariaSort(field: string): 'ascending' | 'descending' | 'none' {
    return ariaSortFor(field, this.sort);
  }

  sortIndicator(field: string): string {
    return sortIndicatorFor(field, this.sort);
  }

  get sortedSuppliers(): Organization[] {
    return sortItems(this.suppliers, this.sort.field, this.sort.direction);
  }

  create(): void {
    if (!this.newName.trim()) return;
    this.creating = true;
    this.orgService.createSupplier(this.newName.trim()).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('ORGS.SUPPLIER_CREATED'));
        this.newName = '';
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
