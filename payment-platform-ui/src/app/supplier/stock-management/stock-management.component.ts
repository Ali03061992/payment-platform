import { Component, OnInit } from '@angular/core';
import { StockService } from '../../services/stock.service';
import { Product } from '../../models/stock.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-stock-management',
  templateUrl: './stock-management.component.html',
  styleUrls: ['./stock-management.component.css']
})
export class StockManagementComponent implements OnInit {
  products: Product[] = [];
  loading = true;
  filterStatus = '';

  constructor(private stockService: StockService, private toast: ToastService) {}

  ngOnInit(): void {
    this.loadProducts();
  }

  loadProducts(): void {
    this.loading = true;
    this.stockService.getProducts(this.filterStatus || undefined).subscribe({
      next: (products) => { this.products = products; this.loading = false; },
      error: (err) => {
        if (err.status === 404) {
          this.products = [];
          this.loading = false;
        } else {
          this.toast.error(err.error?.message || 'Erreur de chargement');
          this.loading = false;
        }
      }
    });
  }

  getStockStatus(product: Product): string {
    if (product.quantity === 0) return 'OUT_OF_STOCK';
    if (product.quantity <= product.minQuantity) return 'LOW';
    return 'OK';
  }

  updateQuantity(product: Product, delta: number): void {
    const newQty = product.quantity + delta;
    if (newQty < 0) return;
    this.stockService.updateProduct(product.id, { quantity: newQty }).subscribe({
      next: (updated) => {
        const idx = this.products.findIndex(p => p.id === product.id);
        if (idx >= 0) this.products[idx] = updated;
        this.toast.success(`${product.name} mis à jour`);
      },
      error: (err) => { this.toast.error(err.error?.message || 'Erreur'); }
    });
  }

  deleteProduct(product: Product): void {
    if (!confirm(`Supprimer ${product.name} ?`)) return;
    this.stockService.deleteProduct(product.id).subscribe({
      next: () => {
        this.products = this.products.filter(p => p.id !== product.id);
        this.toast.success(`${product.name} supprimé`);
      },
      error: (err) => { this.toast.error(err.error?.message || 'Erreur'); }
    });
  }
}
