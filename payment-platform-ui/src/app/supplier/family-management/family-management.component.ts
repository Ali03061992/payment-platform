import { Component, OnInit } from '@angular/core';
import { CatalogService } from '../../services/catalog.service';
import { ProductFamily, ProductCategory } from '../../models/catalog.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-family-management',
  templateUrl: './family-management.component.html',
  styleUrls: ['./family-management.component.css']
})
export class FamilyManagementComponent implements OnInit {
  families: ProductFamily[] = [];
  categories: ProductCategory[] = [];
  loading = true;
  showForm = false;
  saving = false;
  editingFamily: ProductFamily | null = null;
  form = { name: '', code: '', categoryIds: [] as number[] };

  constructor(private catalogService: CatalogService, private toast: ToastService) {}

  ngOnInit(): void { this.loadData(); }

  get supplierId(): number {
    const u = localStorage.getItem('user');
    return u ? JSON.parse(u).organizationId || 0 : 0;
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
      next: (f) => { this.families = f; this.loading = false; },
      error: () => { this.families = []; this.loading = false; }
    });
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

  toggleCategory(catId: number): void {
    const idx = this.form.categoryIds.indexOf(catId);
    if (idx >= 0) {
      this.form.categoryIds.splice(idx, 1);
    } else {
      this.form.categoryIds.push(catId);
    }
  }

  isCategorySelected(catId: number): boolean {
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
    if (!confirm(`Supprimer la famille "${fam.name}" ?`)) return;
    this.catalogService.deleteFamily(fam.id).subscribe({
      next: () => { this.toast.success('Famille supprimee'); this.loadFamilies(); },
      error: (err) => this.toast.error(err.error?.message || 'Erreur')
    });
  }
}
