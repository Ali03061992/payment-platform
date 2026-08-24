import { Component, OnInit } from '@angular/core';
import { StockService } from '../../services/stock.service';
import { CatalogService } from '../../services/catalog.service';
import { Product } from '../../models/stock.model';
import { ProductFamily, ProductSubfamily, ProductCategory } from '../../models/catalog.model';

@Component({
  selector: 'app-product-management',
  templateUrl: './product-management.component.html',
  styleUrls: ['./product-management.component.css']
})
export class ProductManagementComponent implements OnInit {
  products: Product[] = [];
  families: ProductFamily[] = [];
  subfamilies: ProductSubfamily[] = [];
  categories: ProductCategory[] = [];
  loading = true;
  errorMsg = '';
  successMsg = '';

  filterFamily = '';
  filterSubfamily = '';
  filterCategory = '';

  showForm = false;
  editingProduct: Product | null = null;
  saving = false;

  form = {
    name: '',
    sku: '',
    description: '',
    family: '',
    subfamily: '',
    category: '',
    unitPrice: 0,
    currency: 'EUR',
    minQuantity: 0
  };

  constructor(
    private stockService: StockService,
    private catalogService: CatalogService
  ) {}

  ngOnInit(): void {
    this.loadProducts();
    this.loadCatalog();
  }

  get supplierId(): number {
    const userJson = localStorage.getItem('user');
    if (userJson) {
      const user = JSON.parse(userJson);
      return user.organizationId || 0;
    }
    return 0;
  }

  loadProducts(): void {
    this.loading = true;
    this.stockService.getProducts().subscribe({
      next: (data: Product[]) => { this.products = data; this.loading = false; },
      error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur de chargement'; this.loading = false; }
    });
  }

  loadCatalog(): void {
    const sid = this.supplierId;
    this.catalogService.listFamilies(sid).subscribe({
      next: (data) => this.families = data,
      error: () => {}
    });
    this.catalogService.listCategories(sid).subscribe({
      next: (data) => this.categories = data,
      error: () => {}
    });
  }

  onFamilyChange(): void {
    if (this.form.family) {
      const family = this.families.find(f => f.name === this.form.family);
      if (family) {
        this.catalogService.listSubfamilies(this.supplierId, family.id).subscribe({
          next: (data) => this.subfamilies = data,
          error: () => this.subfamilies = []
        });
      }
    } else {
      this.subfamilies = [];
      this.form.subfamily = '';
    }
  }

  get filteredProducts(): Product[] {
    return this.products.filter(p => {
      if (this.filterCategory && (p as any).category !== this.filterCategory) return false;
      return true;
    });
  }

  openCreate(): void {
    this.editingProduct = null;
    this.form = { name: '', sku: '', description: '', family: '', subfamily: '', category: '', unitPrice: 0, currency: 'EUR', minQuantity: 0 };
    this.showForm = true;
  }

  openEdit(product: Product): void {
    this.editingProduct = product;
    this.form = {
      name: product.name,
      sku: product.sku,
      description: product.description || '',
      family: (product as any).family || '',
      subfamily: (product as any).subfamily || '',
      category: (product as any).category || '',
      unitPrice: product.unitPrice,
      currency: product.currency,
      minQuantity: product.minQuantity
    };
    this.showForm = true;
  }

  save(): void {
    this.saving = true;
    this.errorMsg = '';

    if (this.editingProduct) {
      this.stockService.updateProduct(this.editingProduct.id, {
        name: this.form.name,
        description: this.form.description,
        unitPrice: this.form.unitPrice,
        minQuantity: this.form.minQuantity
      }).subscribe({
        next: () => {
          this.successMsg = 'Produit mis à jour avec succès';
          this.showForm = false;
          this.saving = false;
          this.loadProducts();
          setTimeout(() => this.successMsg = '', 3000);
        },
        error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur lors de la mise à jour'; this.saving = false; }
      });
    } else {
      this.stockService.createProduct({
        name: this.form.name,
        sku: this.form.sku,
        description: this.form.description,
        unitPrice: this.form.unitPrice,
        currency: this.form.currency,
        quantity: 0,
        minQuantity: this.form.minQuantity
      }).subscribe({
        next: () => {
          this.successMsg = 'Produit créé avec succès';
          this.showForm = false;
          this.saving = false;
          this.loadProducts();
          setTimeout(() => this.successMsg = '', 3000);
        },
        error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur lors de la création'; this.saving = false; }
      });
    }
  }

  cancelForm(): void {
    this.showForm = false;
    this.editingProduct = null;
    this.errorMsg = '';
  }

  toggleStatus(product: Product): void {
    const newStatus = product.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.stockService.updateProduct(product.id, { status: newStatus }).subscribe({
      next: () => { this.loadProducts(); },
      error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur'; setTimeout(() => this.errorMsg = '', 3000); }
    });
  }

  deleteProduct(product: Product): void {
    if (!confirm(`Supprimer le produit "${product.name}" ?`)) return;
    this.stockService.deleteProduct(product.id).subscribe({
      next: () => {
        this.successMsg = 'Produit supprimé';
        this.loadProducts();
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur'; setTimeout(() => this.errorMsg = '', 3000); }
    });
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = { ACTIVE: 'Actif', INACTIVE: 'Inactif', OUT_OF_STOCK: 'Rupture' };
    return map[s] || s;
  }

  getFamily(p: Product): string {
    return (p as any).family || '-';
  }

  getReserved(p: Product): number {
    return (p as any).reservedQuantity || 0;
  }

  getAvailable(p: Product): number {
    return p.quantity - ((p as any).reservedQuantity || 0);
  }
}
