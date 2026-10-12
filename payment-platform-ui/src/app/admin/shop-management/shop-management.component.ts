import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { OrganizationService } from '../../services/organization.service';
import { FISCAL_REGIMES, FiscalRegime, Organization } from '../../models/organization.model';
import { PageResponse, sortItems, toggleSortState, ariaSortFor, sortIndicatorFor, SortState } from '../../models/page.model';
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
  fiscalRegimes = FISCAL_REGIMES;
  fiscalForm = ShopManagementComponent.emptyFiscalForm();
  editingShop: Organization | null = null;
  showFiscalEdit = false;
  savingFiscal = false;

  static emptyFiscalForm() {
    return {
      matriculeFiscal: '', patente: '', tejIdentifier: '', address: '', fiscalRegime: 'ASSUJETTI' as FiscalRegime,
      suspensionAttestationNumber: '', suspensionAttestationDate: '', suspensionExpiryDate: ''
    };
  }

  /** MF exigible sauf particulier (NON_ASSUJETTI) ou client export. */
  mfRequired(): boolean {
    return this.fiscalForm.fiscalRegime !== 'NON_ASSUJETTI'
      && this.fiscalForm.fiscalRegime !== 'EXPORT';
  }

  canCreate(): boolean {
    if (!this.newName.trim()) return false;
    return !this.mfRequired() || !!this.fiscalForm.matriculeFiscal.trim();
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

  onSort(field: string): void {
    this.sort = toggleSortState(this.sort, field);
  }

  ariaSort(field: string): 'ascending' | 'descending' | 'none' {
    return ariaSortFor(field, this.sort);
  }

  sortIndicator(field: string): string {
    return sortIndicatorFor(field, this.sort);
  }

  get sortedShops(): Organization[] {
    return sortItems(this.shops, this.sort.field, this.sort.direction);
  }

  create(): void {
    if (!this.canCreate()) return;
    this.creating = true;
    const f = this.fiscalForm;
    this.orgService.createShop({
      name: this.newName.trim(),
      matriculeFiscal: f.matriculeFiscal.trim() || null,
      patente: f.patente.trim() || null,
      tejIdentifier: f.tejIdentifier.trim() || null,
      address: f.address.trim() || null,
      fiscalRegime: f.fiscalRegime,
      suspensionAttestationNumber: f.suspensionAttestationNumber.trim() || null,
      suspensionAttestationDate: f.suspensionAttestationDate || null,
      suspensionExpiryDate: f.suspensionExpiryDate || null
    }).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('ORGS.SHOP_CREATED'));
        this.newName = '';
        this.fiscalForm = ShopManagementComponent.emptyFiscalForm();
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
    this.editingShop = s;
    this.fiscalForm = {
      matriculeFiscal: s.matriculeFiscal || '', patente: s.patente || '', tejIdentifier: s.tejIdentifier || '',
      address: s.address || '',
      fiscalRegime: (s.fiscalRegime as FiscalRegime) || 'ASSUJETTI',
      suspensionAttestationNumber: s.suspensionAttestationNumber || '',
      suspensionAttestationDate: s.suspensionAttestationDate || '',
      suspensionExpiryDate: s.suspensionExpiryDate || ''
    };
    this.showFiscalEdit = true;
  }

  saveFiscalEdit(): void {
    if (!this.editingShop) return;
    this.savingFiscal = true;
    const f = this.fiscalForm;
    this.orgService.updateFiscalProfile(this.editingShop.id, 'SHOP', {
      name: this.editingShop.name,
      matriculeFiscal: f.matriculeFiscal.trim() || null,
      patente: f.patente.trim() || null,
      tejIdentifier: f.tejIdentifier.trim() || null,
      address: f.address.trim() || null,
      fiscalRegime: f.fiscalRegime,
      suspensionAttestationNumber: f.suspensionAttestationNumber.trim() || null,
      suspensionAttestationDate: f.suspensionAttestationDate || null,
      suspensionExpiryDate: f.suspensionExpiryDate || null
    }).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('ORGS.FISCAL_UPDATED'));
        this.showFiscalEdit = false;
        this.editingShop = null;
        this.savingFiscal = false;
        this.load();
      },
      error: (err: { error?: { message?: string } }) => {
        this.toast.error(err.error?.message || this.translate.instant('ORGS.CREATE_ERROR'));
        this.savingFiscal = false;
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
