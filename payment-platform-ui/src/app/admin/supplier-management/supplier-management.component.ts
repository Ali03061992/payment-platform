import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { OrganizationService } from '../../services/organization.service';
import { FISCAL_REGIMES, FiscalRegime, Organization } from '../../models/organization.model';
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
  fiscalRegimes = FISCAL_REGIMES;
  fiscalForm = SupplierManagementComponent.emptyFiscalForm();
  editingSupplier: Organization | null = null;
  showFiscalEdit = false;
  savingFiscal = false;

  static emptyFiscalForm() {
    return {
      matriculeFiscal: '', patente: '', tejIdentifier: '', defaultWithholdingNature: '',
      address: '', fiscalRegime: 'ASSUJETTI' as FiscalRegime,
      suspensionAttestationNumber: '', suspensionAttestationDate: '', suspensionExpiryDate: ''
    };
  }
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

  /** MF + adresse obligatoires pour un fournisseur (art. 18 Code TVA). */
  canCreate(): boolean {
    return !!this.newName.trim() && !!this.fiscalForm.matriculeFiscal.trim()
      && !!this.fiscalForm.address.trim();
  }

  create(): void {
    if (!this.canCreate()) return;
    this.creating = true;
    const f = this.fiscalForm;
    this.orgService.createSupplier({
      name: this.newName.trim(),
      matriculeFiscal: f.matriculeFiscal.trim(),
      patente: f.patente.trim() || null,
      tejIdentifier: f.tejIdentifier.trim() || null,
      defaultWithholdingNature: f.defaultWithholdingNature.trim() || null,
      address: f.address.trim(),
      fiscalRegime: f.fiscalRegime,
      suspensionAttestationNumber: f.suspensionAttestationNumber.trim() || null,
      suspensionAttestationDate: f.suspensionAttestationDate || null,
      suspensionExpiryDate: f.suspensionExpiryDate || null
    }).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('ORGS.SUPPLIER_CREATED'));
        this.newName = '';
        this.fiscalForm = SupplierManagementComponent.emptyFiscalForm();
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

  openFiscalEdit(s: Organization): void {
    this.editingSupplier = s;
    this.fiscalForm = {
      matriculeFiscal: s.matriculeFiscal || '', patente: s.patente || '', tejIdentifier: s.tejIdentifier || '',
      defaultWithholdingNature: s.defaultWithholdingNature || '',
      address: s.address || '',
      fiscalRegime: (s.fiscalRegime as FiscalRegime) || 'ASSUJETTI',
      suspensionAttestationNumber: s.suspensionAttestationNumber || '',
      suspensionAttestationDate: s.suspensionAttestationDate || '',
      suspensionExpiryDate: s.suspensionExpiryDate || ''
    };
    this.showFiscalEdit = true;
  }

  saveFiscalEdit(): void {
    if (!this.editingSupplier || !this.fiscalForm.matriculeFiscal.trim()) return;
    this.savingFiscal = true;
    const f = this.fiscalForm;
    this.orgService.updateFiscalProfile(this.editingSupplier.id, 'SUPPLIER', {
      name: this.editingSupplier.name,
      matriculeFiscal: f.matriculeFiscal.trim(),
      patente: f.patente.trim() || null,
      tejIdentifier: f.tejIdentifier.trim() || null,
      defaultWithholdingNature: f.defaultWithholdingNature.trim() || null,
      address: f.address.trim() || null,
      fiscalRegime: f.fiscalRegime,
      suspensionAttestationNumber: f.suspensionAttestationNumber.trim() || null,
      suspensionAttestationDate: f.suspensionAttestationDate || null,
      suspensionExpiryDate: f.suspensionExpiryDate || null
    }).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('ORGS.FISCAL_UPDATED'));
        this.showFiscalEdit = false;
        this.editingSupplier = null;
        this.savingFiscal = false;
        this.load();
      },
      error: (err: { error?: { message?: string } }) => {
        this.toast.error(err.error?.message || this.translate.instant('ORGS.CREATE_ERROR'));
        this.savingFiscal = false;
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
