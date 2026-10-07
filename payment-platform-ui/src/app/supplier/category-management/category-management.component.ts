import { Component, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { CatalogService } from '../../services/catalog.service';
import { ProductCategory } from '../../models/catalog.model';
import { ToastService } from '../../services/toast.service';
import { ConfirmDialogService } from '../../components/confirm-dialog/confirm-dialog.service';
import { paginateItems, sortItems, toggleSortState, ariaSortFor, sortIndicatorFor, SortState } from '../../models/page.model';

@Component({
    selector: 'app-category-management',
    templateUrl: './category-management.component.html',
    styleUrls: ['./category-management.component.css'],
    standalone: false
})
export class CategoryManagementComponent implements OnInit {
  categories: ProductCategory[] = [];
  loading = true;
  showForm = false;
  saving = false;
  form = { name: '', code: '' };
  currentPage = 0;
  pageSize = 10;
  sort: SortState = { field: null, direction: 'asc' };

  constructor(private catalogService: CatalogService, private toast: ToastService, private confirmDialog: ConfirmDialogService, private translate: TranslateService) {}

  ngOnInit(): void { this.loadCategories(); }

  get supplierId(): string {
    const u = sessionStorage.getItem('user');
    return u ? JSON.parse(u).organizationId || '' : '';
  }

  loadCategories(): void {
    this.loading = true;
    this.catalogService.listCategories(this.supplierId).subscribe({
      next: (data) => { this.categories = data; this.currentPage = 0; this.loading = false; },
      error: () => { this.categories = []; this.loading = false; }
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

  get sortedCategories(): ProductCategory[] {
    return sortItems(this.categories, this.sort.field, this.sort.direction);
  }

  get pagedCategories(): ProductCategory[] {
    return paginateItems(this.sortedCategories, this.currentPage, this.pageSize);
  }

  onPageChange(page: number): void {
    this.currentPage = page;
  }

  onSizeChange(size: number): void {
    this.pageSize = size;
    this.currentPage = 0;
  }

  openCreate(): void {
    this.form = { name: '', code: '' };
    this.showForm = true;
  }

  save(): void {
    if (!this.form.name.trim() || !this.form.code.trim()) return;
    this.saving = true;
    this.catalogService.createCategory({ supplierId: this.supplierId, name: this.form.name, code: this.form.code }).subscribe({
      next: () => {
        this.toast.success(this.translate.instant('CATALOG.CATEGORY_CREATED'));
        this.showForm = false;
        this.saving = false;
        this.loadCategories();
      },
      error: (err) => { this.toast.error(err.error?.message || this.translate.instant('STOCK.ERROR')); this.saving = false; }
    });
  }

  deleteCategory(cat: ProductCategory): void {
    this.confirmDialog.confirm({
      title: this.translate.instant('CATALOG.DELETE_CATEGORY_TITLE'),
      message: this.translate.instant('CATALOG.DELETE_CATEGORY_MSG', { name: cat.name }),
      danger: true,
    }).subscribe(ok => {
      if (!ok) return;
      this.catalogService.deleteCategory(cat.id).subscribe({
        next: () => { this.toast.success(this.translate.instant('CATALOG.CATEGORY_DELETED')); this.loadCategories(); },
        error: (err) => this.toast.error(err.error?.message || this.translate.instant('STOCK.ERROR'))
      });
    });
  }
}
