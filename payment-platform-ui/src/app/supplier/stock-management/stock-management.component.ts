import { Component, OnInit } from '@angular/core';
import { StockService } from '../../services/stock.service';
import { Product, StockMovement } from '../../models/stock.model';
import { ToastService } from '../../services/toast.service';

@Component({
    selector: 'app-stock-management',
    templateUrl: './stock-management.component.html',
    styleUrls: ['./stock-management.component.css'],
    standalone: false
})
export class StockManagementComponent implements OnInit {
  products: Product[] = [];
  filteredProducts: Product[] = [];
  recentMovements: StockMovement[] = [];
  loading = true;
  searchTerm = '';
  filterStatus = '';
  sortBy = 'lowStock';

  stats = { total: 0, totalQty: 0, totalValue: 0, lowStock: 0, outOfStock: 0 };

  showMovementModal = false;
  selectedProduct: Product | null = null;
  movementType: 'IN' | 'OUT' | 'ADJUSTMENT' = 'IN';
  movementQty = 0;
  movementRef = '';
  movementNotes = '';
  savingMovement = false;

  showHistoryModal = false;
  historyProduct: Product | null = null;
  historyMovements: StockMovement[] = [];
  loadingHistory = false;

  constructor(private stockService: StockService, private toast: ToastService) {}

  ngOnInit(): void { this.loadData(); }

  loadData(): void {
    this.loading = true;
    this.stockService.getProducts().subscribe({
      next: (p) => { this.products = p; this.applyFilters(); this.loadMovements(); this.loading = false; },
      error: () => { this.products = []; this.filteredProducts = []; this.loading = false; }
    });
  }

  loadMovements(): void {
    this.stockService.getStockMovements().subscribe({
      next: (m) => this.recentMovements = m.slice(0, 10),
      error: () => {}
    });
  }

  applyFilters(): void {
    let result = [...this.products];
    if (this.searchTerm.trim()) {
      const t = this.searchTerm.toLowerCase();
      result = result.filter(p => p.name.toLowerCase().includes(t) || p.sku.toLowerCase().includes(t));
    }
    if (this.filterStatus) {
      result = result.filter(p => {
        const s = this.getStockStatus(p);
        return this.filterStatus === 'LOW' ? s === 'LOW' : this.filterStatus === 'OUT_OF_STOCK' ? s === 'OUT_OF_STOCK' : s === 'OK';
      });
    }
    const order: { [k: string]: number } = { OUT_OF_STOCK: 0, LOW: 1, OK: 2 };
    result.sort((a, b) => {
      switch (this.sortBy) {
        case 'lowStock': return (order[this.getStockStatus(a)] || 2) - (order[this.getStockStatus(b)] || 2);
        case 'quantity': return a.quantity - b.quantity;
        case 'name': default: return a.name.localeCompare(b.name);
      }
    });
    this.filteredProducts = result;
    this.computeStats();
  }

  computeStats(): void {
    this.stats.total = this.products.length;
    this.stats.totalQty = this.products.reduce((s, p) => s + p.quantity, 0);
    this.stats.totalValue = this.products.reduce((s, p) => s + (p.unitPrice * p.quantity), 0);
    this.stats.lowStock = this.products.filter(p => this.getStockStatus(p) === 'LOW').length;
    this.stats.outOfStock = this.products.filter(p => this.getStockStatus(p) === 'OUT_OF_STOCK').length;
  }

  getStockStatus(p: Product): string {
    if (p.quantity === 0) return 'OUT_OF_STOCK';
    if (p.quantity <= p.minQuantity) return 'LOW';
    return 'OK';
  }

  getStockPercent(p: Product): number {
    if (p.minQuantity <= 0) return 100;
    return Math.min(100, Math.round((p.quantity / (p.minQuantity * 3)) * 100));
  }

  openMovement(product: Product, type: 'IN' | 'OUT' | 'ADJUSTMENT'): void {
    this.selectedProduct = product;
    this.movementType = type;
    this.movementQty = 0;
    this.movementRef = '';
    this.movementNotes = '';
    this.showMovementModal = true;
  }

  closeMovement(): void { this.showMovementModal = false; this.selectedProduct = null; }

  submitMovement(): void {
    if (!this.selectedProduct || this.movementQty <= 0) return;
    this.savingMovement = true;
    this.stockService.createStockMovement(this.selectedProduct.id, {
      type: this.movementType, quantity: this.movementQty, reference: this.movementRef, notes: this.movementNotes
    }).subscribe({
      next: () => { this.toast.success('Mouvement enregistre'); this.closeMovement(); this.savingMovement = false; this.loadData(); },
      error: (err) => { this.toast.error(err.error?.message || 'Erreur'); this.savingMovement = false; }
    });
  }

  openHistory(p: Product): void {
    this.historyProduct = p;
    this.loadingHistory = true;
    this.showHistoryModal = true;
    this.stockService.getStockMovements(p.id).subscribe({
      next: (d) => { this.historyMovements = d; this.loadingHistory = false; },
      error: () => { this.historyMovements = []; this.loadingHistory = false; }
    });
  }

  closeHistory(): void { this.showHistoryModal = false; this.historyProduct = null; }

  statusLabel(s: string): string { return ({ OK: 'En stock', LOW: 'Stock bas', OUT_OF_STOCK: 'Rupture' } as Record<string, string>)[s] || s; }
  movementTypeLabel(t: string): string { return ({ IN: 'Entree', OUT: 'Sortie', ADJUSTMENT: 'Ajustement' } as Record<string, string>)[t] || t; }
  movementTypeClass(t: string): string { return ({ IN: 'movement-in', OUT: 'movement-out', ADJUSTMENT: 'movement-adjust' } as Record<string, string>)[t] || ''; }
}
