import { Component, OnInit } from '@angular/core';
import { CatalogService } from '../../services/catalog.service';
import { ProductFamily, ProductCategory } from '../../models/catalog.model';
import { ToastService } from '../../services/toast.service';
import { ConfirmDialogService } from '../../components/confirm-dialog/confirm-dialog.service';
import { paginateItems, sortItems, toggleSortState, ariaSortFor, sortIndicatorFor, SortState } from '../../models/page.model';

@Component({
    selector: 'app-family-management',
    templateUrl: './family-management.component.html',
    styleUrls: ['./family-management.component.css'],
    standalone: false
})
export class FamilyManagementComponent implements OnInit {
  families: ProductFamily[] = [];
  categories: ProductCategory[] = [];
  loading = true;
  showForm = false;
  saving = false;
  editingFamily: ProductFamily | null = null;
  form = { name: '', code: '', categoryIds: [] as string[] };
  currentPage = 0;
  pageSize = 10;
  sort: SortState = { field: null, direction: 'asc' };

  constructor(private catalogService: CatalogService, private toast: ToastService, private confirmDialog: ConfirmDialogService) {}

  ngOnInit(): void { this.loadData(); }

  get supplierId(): string {
    const u = sessionStorage.getItem('user');
    return u ? JSON.parse(u).organizationId || '' : '';
  }

  loadData(): void {
    this.loading = true;
    this.catalogService.listCategories(this.supplierId).subscribe({
      next: (c) => { this.categories = c; this.loadFamilies(); },
      error: () => { this.categories = []; this.loadFamilies(); }
    });
  }

  loadFamilies(): void {
    this.catalogService.listFamilies(this.supplierId).subscribe({
      next: (f) => { this.families = f; this.currentPage = 0; this.loading = false; },
      error: () => { this.families = []; this.loading = false; }
    });
  }

  /** Tri côté client sur la page chargée (backend sans tri serveur). */
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

  get sortedFamilies(): ProductFamily[] {
    return sortItems(this.families, this.sort.field, this.sort.direction);
  }

  get pagedFamilies(): ProductFamily[] {
    return paginateItems(this.sortedFamilies, this.currentPage, this.pageSize);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
  }

  onSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
  }

  getCategoryNames(family: ProductFamily): string {
    if (!family.categories || family.categories.length === 0) return '-';
    return family.categories.map(c => c.name).join(', ');
  }

  openCreate(): void {
    this.editingFamily = null;
    this.form = { name: '', code: '', categoryIds: [] };
    this.showForm = true;
  }

  openEdit(family: ProductFamily): void {
    this.editingFamily = family;
    this.form = {
      name: family.name,
      code: family.code,
      categoryIds: family.categories ? family.categories.map(c => c.id) : []
    };
    this.showForm = true;
  }

  toggleCategory(catId: string): void {
    const idx = this.form.categoryIds.indexOf(catId);
    if (idx >= 0) {
      this.form.categoryIds.splice(idx, 1);
    } else {
      this.form.categoryIds.push(catId);
    }
  }

  isCategorySelected(catId: string): boolean {
    return this.form.categoryIds.includes(catId);
  }

  save(): void {
    if (!this.form.name.trim() || !this.form.code.trim()) return;
    this.saving = true;

    const data = { supplierId: this.supplierId, name: this.form.name, code: this.form.code, categoryIds: this.form.categoryIds };

    if (this.editingFamily) {
      this.catalogService.updateFamily(this.editingFamily.id, data).subscribe({
        next: () => { this.toast.success('Famille mise a jour'); this.showForm = false; this.saving = false; this.loadFamilies(); },
        error: (err) => { this.toast.error(err.error?.message || 'Erreur'); this.saving = false; }
      });
    } else {
      this.catalogService.createFamily(data).subscribe({
        next: () => { this.toast.success('Famille creee'); this.showForm = false; this.saving = false; this.loadFamilies(); },
        error: (err) => { this.toast.error(err.error?.message || 'Erreur'); this.saving = false; }
      });
    }
  }

  deleteFamily(fam: ProductFamily): void {
    this.confirmDialog.confirm({
      title: 'Supprimer la famille',
      message: `Supprimer la famille "${fam.name}" ?`,
      danger: true,
    }).subscribe(ok => {
      if (!ok) return;
      this.catalogService.deleteFamily(fam.id).subscribe({
        next: () => { this.toast.success('Famille supprimee'); this.loadFamilies(); },
        error: (err) => this.toast.error(err.error?.message || 'Erreur')
      });
    });
  }
}
