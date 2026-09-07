import { Component, OnInit } from '@angular/core';
import { CatalogService } from '../../services/catalog.service';
import { ProductCategory } from '../../models/catalog.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-category-management',
  templateUrl: './category-management.component.html',
  styleUrls: ['./category-management.component.css']
})
export class CategoryManagementComponent implements OnInit {
  categories: ProductCategory[] = [];
  loading = true;
  showForm = false;
  saving = false;
  form = { name: '', code: '' };

  constructor(private catalogService: CatalogService, private toast: ToastService) {}

  ngOnInit(): void { this.loadCategories(); }

  get supplierId(): number {
    const u = sessionStorage.getItem('user');
    return u ? JSON.parse(u).organizationId || 0 : 0;
  }

  loadCategories(): void {
    this.loading = true;
    this.catalogService.listCategories(this.supplierId).subscribe({
      next: (data) => { this.categories = data; this.loading = false; },
      error: () => { this.categories = []; this.loading = false; }
    });
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
        this.toast.success('Categorie creee');
        this.showForm = false;
        this.saving = false;
        this.loadCategories();
      },
      error: (err) => { this.toast.error(err.error?.message || 'Erreur'); this.saving = false; }
    });
  }

  deleteCategory(cat: ProductCategory): void {
    if (!confirm(`Supprimer la categorie "${cat.name}" ?`)) return;
    this.catalogService.deleteCategory(cat.id).subscribe({
      next: () => { this.toast.success('Categorie supprimee'); this.loadCategories(); },
      error: (err) => this.toast.error(err.error?.message || 'Erreur')
    });
  }
}
