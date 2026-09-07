import { Component, OnInit } from '@angular/core';
import { StockOptimizationService } from '../../services/stock-optimization.service';
import { StockOptimizationResponse, ProductOptimization } from '../../models/stock-optimization.model';
import { ToastService } from '../../services/toast.service';

@Component({
  selector: 'app-stock-optimization',
  templateUrl: './stock-optimization.component.html',
  styleUrls: ['./stock-optimization.component.css']
})
export class StockOptimizationComponent implements OnInit {
  data: StockOptimizationResponse | null = null;
  loading = true;
  error = '';
  activeTab = 'overview';
  selectedProduct: ProductOptimization | null = null;

  // Config
  showConfig = false;
  configLeadTime = 7;
  configOrderingCost = 50;
  configHoldingCost = 25;

  constructor(private optimizationService: StockOptimizationService, private toast: ToastService) {}

  ngOnInit(): void { this.loadOptimization(); }

  loadOptimization(): void {
    this.loading = true;
    this.error = '';
    this.optimizationService.optimize().subscribe({
      next: (data) => { this.data = data; this.loading = false; },
      error: (err) => { this.error = err.error?.message || 'Erreur de chargement'; this.loading = false; }
    });
  }

  applyConfig(): void {
    this.loading = true;
    this.optimizationService.configure(this.configLeadTime, this.configOrderingCost, this.configHoldingCost / 100).subscribe({
      next: (data) => { this.data = data; this.showConfig = false; this.loading = false; this.toast.success('Parametres applies'); },
      error: (err) => { this.toast.error(err.error?.message || 'Erreur'); this.loading = false; }
    });
  }

  selectProduct(p: ProductOptimization): void {
    this.selectedProduct = this.selectedProduct?.productId === p.productId ? null : p;
  }

  abcColor(c: string): string {
    return ({ A: '#c62828', B: '#e65100', C: '#2e7d32' } as Record<string, string>)[c] || '#666';
  }

  abcBg(c: string): string {
    return ({ A: '#fce4ec', B: '#fff3e0', C: '#e8f5e9' } as Record<string, string>)[c] || '#f5f5f5';
  }

  xyzColor(c: string): string {
    return ({ X: '#2e7d32', Y: '#e65100', Z: '#c62828' } as Record<string, string>)[c] || '#666';
  }

  actionColor(a: string): string {
    const m: Record<string, string> = { ORDER_NOW: '#c62828', ORDER_SOON: '#e65100', MONITOR: '#f9a825', REDUCE_STOCK: '#1565c0', NO_ACTION: '#2e7d32', INVESTIGATE: '#6a1b9a', DISCONTINUE: '#546e7a' };
    return m[a] || '#666';
  }

  riskColor(r: string): string {
    return ({ CRITICAL: '#c62828', HIGH: '#e65100', MEDIUM: '#f9a825', LOW: '#2e7d32' } as Record<string, string>)[r] || '#666';
  }

  formatPercent(v: number): string { return (v * 100).toFixed(1) + '%'; }
  formatNumber(v: number): string { return v.toFixed(1); }
}
