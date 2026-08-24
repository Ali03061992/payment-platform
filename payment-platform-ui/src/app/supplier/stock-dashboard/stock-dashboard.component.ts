import { Component, OnInit } from '@angular/core';
import { StockService } from '../../services/stock.service';
import { Product, StockMovement } from '../../models/stock.model';

@Component({
  selector: 'app-stock-dashboard',
  templateUrl: './stock-dashboard.component.html',
  styleUrls: ['./stock-dashboard.component.css']
})
export class StockDashboardComponent implements OnInit {
  products: Product[] = [];
  movements: StockMovement[] = [];
  loading = true;
  errorMsg = '';
  successMsg = '';

  totalProducts = 0;
  totalQuantity = 0;
  totalValue = 0;
  lowStock = 0;
  outOfStock = 0;

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

  constructor(private stockService: StockService) {}

  ngOnInit(): void {
    this.loadData();
  }

  loadData(): void {
    this.loading = true;
    this.stockService.getProducts().subscribe({
      next: (data: Product[]) => {
        this.products = data;
        this.computeStats();
        this.loading = false;
      },
      error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur de chargement'; this.loading = false; }
    });
  }

  computeStats(): void {
    this.totalProducts = this.products.length;
    this.totalQuantity = this.products.reduce((sum, p) => sum + p.quantity, 0);
    this.totalValue = this.products.reduce((sum, p) => sum + (p.quantity * p.unitPrice), 0);
    this.lowStock = this.products.filter(p => p.quantity > 0 && p.quantity <= p.minQuantity).length;
    this.outOfStock = this.products.filter(p => p.quantity === 0).length;
  }

  openMovement(product: Product, type: 'IN' | 'OUT' | 'ADJUSTMENT'): void {
    this.selectedProduct = product;
    this.movementType = type;
    this.movementQty = 0;
    this.movementRef = '';
    this.movementNotes = '';
    this.showMovementModal = true;
  }

  closeMovement(): void {
    this.showMovementModal = false;
    this.selectedProduct = null;
  }

  submitMovement(): void {
    if (!this.selectedProduct || this.movementQty <= 0) return;
    this.savingMovement = true;
    this.stockService.createStockMovement(this.selectedProduct.id, {
      type: this.movementType,
      quantity: this.movementQty,
      reference: this.movementRef,
      notes: this.movementNotes
    }).subscribe({
      next: () => {
        this.successMsg = 'Mouvement de stock enregistré';
        this.closeMovement();
        this.savingMovement = false;
        this.loadData();
        setTimeout(() => this.successMsg = '', 3000);
      },
      error: (err: any) => { this.errorMsg = err.error?.message || 'Erreur'; this.savingMovement = false; setTimeout(() => this.errorMsg = '', 3000); }
    });
  }

  openHistory(product: Product): void {
    this.historyProduct = product;
    this.loadingHistory = true;
    this.showHistoryModal = true;
    this.stockService.getStockMovements(product.id).subscribe({
      next: (data) => { this.historyMovements = data; this.loadingHistory = false; },
      error: () => { this.historyMovements = []; this.loadingHistory = false; }
    });
  }

  closeHistory(): void {
    this.showHistoryModal = false;
    this.historyProduct = null;
    this.historyMovements = [];
  }

  movementTypeLabel(type: string): string {
    const map: Record<string, string> = { IN: 'Entrée', OUT: 'Sortie', ADJUSTMENT: 'Ajustement' };
    return map[type] || type;
  }

  movementTypeClass(type: string): string {
    const map: Record<string, string> = { IN: 'movement-in', OUT: 'movement-out', ADJUSTMENT: 'movement-adjust' };
    return map[type] || '';
  }

  getStockStatus(product: Product): string {
    if (product.quantity === 0) return 'OUT_OF_STOCK';
    if (product.quantity <= product.minQuantity) return 'LOW';
    return 'OK';
  }
}
