import { Component, OnInit } from '@angular/core';
import { StockService } from '../../services/stock.service';
import { CatalogService } from '../../services/catalog.service';
import { Product } from '../../models/stock.model';
import { ProductFamily, ProductCategory } from '../../models/catalog.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-product-management',
  templateUrl: './product-management.component.html',
  styleUrls: ['./product-management.component.css']
})
export class ProductManagementComponent implements OnInit {
  products: Product[] = [];
  families: ProductFamily[] = [];
  categories: ProductCategory[] = [];
  loading = true;
  filterCategory = '';
  filterFamily = '';
  showForm = false;
  editingProduct: Product | null = null;
  saving = false;

  form = {
    name: '', sku: '', description: '', unitPrice: 0, currency: 'TND',
    categoryId: 0, familyId: 0, unit: 'unite', minQuantity: 0
  };

  constructor(
    private stockService: StockService,
    private catalogService: CatalogService,
    private toast: ToastService
  ) {}

  ngOnInit(): void { this.loadData(); }

  get supplierId(): number {
    const u = localStorage.getItem('user');
    return u ? JSON.parse(u).organizationId || 0 : 0;
  }

  loadData(): void {
    this.loading = true;
    this.stockService.getProducts().subscribe({
      next: (p) => { this.products = p; this.loading = false; },
      error: () => { this.products = []; this.loading = false; }
    });
    const sid = this.supplierId;
    this.catalogService.listCategories(sid).subscribe({ next: (c) => this.categories = c, error: () => {} });
    this.catalogService.listFamilies(sid).subscribe({ next: (f) => this.families = f, error: () => {} });
  }

  get filteredProducts(): Product[] {
    return this.products.filter(p => {
      if (this.filterCategory && p.categoryId !== +this.filterCategory) return false;
      if (this.filterFamily && p.familyId !== +this.filterFamily) return false;
      return true;
    });
  }

  getCategoryName(id: number | null): string { return this.categories.find(c => c.id === id)?.name || '-'; }
  getFamilyName(id: number | null): string { return this.families.find(f => f.id === id)?.name || '-'; }

  onCategoryChange(): void {
    this.form.familyId = 0;
  }

  getFamiliesForCategory(): ProductFamily[] {
    if (!this.form.categoryId) return this.families;
    return this.families.filter(f => f.categories && f.categories.some(c => c.id === this.form.categoryId));
  }

  openCreate(): void {
    this.editingProduct = null;
    this.form = { name: '', sku: '', description: '', unitPrice: 0, currency: 'TND', categoryId: 0, familyId: 0, unit: 'unite', minQuantity: 0 };
    this.showForm = true;
  }

  openEdit(p: Product): void {
    this.editingProduct = p;
    this.form = {
      name: p.name, sku: p.sku, description: p.description || '', unitPrice: p.unitPrice,
      currency: p.currency, categoryId: p.categoryId || 0, familyId: p.familyId || 0,
      unit: p.unit || 'unite', minQuantity: p.minQuantity
    };
    this.showForm = true;
  }

  save(): void {
    if (!this.form.name.trim() || !this.form.sku.trim()) return;
    this.saving = true;

    const data = {
      name: this.form.name, sku: this.form.sku, description: this.form.description,
      unitPrice: this.form.unitPrice, currency: this.form.currency,
      categoryId: this.form.categoryId || null, familyId: this.form.familyId || null,
      unit: this.form.unit, minQuantity: this.form.minQuantity
    };

    if (this.editingProduct) {
      this.stockService.updateProduct(this.editingProduct.id, data).subscribe({
        next: () => { this.toast.success('Produit mis a jour'); this.showForm = false; this.saving = false; this.loadData(); },
        error: (err) => { this.toast.error(err.error?.message || 'Erreur'); this.saving = false; }
      });
    } else {
      this.stockService.createProduct({ ...data, quantity: 0 }).subscribe({
        next: () => { this.toast.success('Produit cree'); this.showForm = false; this.saving = false; this.loadData(); },
        error: (err) => { this.toast.error(err.error?.message || 'Erreur'); this.saving = false; }
      });
    }
  }

  toggleStatus(p: Product): void {
    const newStatus = p.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.stockService.updateProduct(p.id, { status: newStatus }).subscribe({
      next: () => this.loadData(),
      error: (err) => this.toast.error(err.error?.message || 'Erreur')
    });
  }

  deleteProduct(p: Product): void {
    if (!confirm(`Supprimer "${p.name}" ?`)) return;
    this.stockService.deleteProduct(p.id).subscribe({
      next: () => { this.toast.success('Produit supprime'); this.loadData(); },
      error: (err) => this.toast.error(err.error?.message || 'Erreur')
    });
  }

  statusLabel(s: string): string {
    return ({ ACTIVE: 'Actif', INACTIVE: 'Inactif', OUT_OF_STOCK: 'Hors stock' } as Record<string, string>)[s] || s;
  }
}
