import { Component, OnInit } from '@angular/core';
import { StockService } from '../../services/stock.service';
import { Product } from '../../models/stock.model';

@Component({
    selector: 'app-low-stock-alerts',
    templateUrl: './low-stock-alerts.component.html',
    styleUrls: ['./low-stock-alerts.component.css'],
    standalone: false
})
export class LowStockAlertsComponent implements OnInit {
  alerts: Product[] = [];
  loading = true;

  constructor(private stockService: StockService) {}

  ngOnInit(): void {
    this.loadAlerts();
  }

  loadAlerts(): void {
    this.loading = true;
    this.stockService.getLowStockAlerts().subscribe({
      next: (products) => {
        this.alerts = this.sortByUrgency(products);
        this.loading = false;
      },
      error: () => {
        this.alerts = [];
        this.loading = false;
      }
    });
  }

  private sortByUrgency(products: Product[]): Product[] {
    return [...products].sort((a, b) => {
      const availableA = a.quantity - a.reservedQty;
      const availableB = b.quantity - b.reservedQty;
      const deficitA = a.minQuantity - availableA;
      const deficitB = b.minQuantity - availableB;
      if (availableA === 0 && availableB > 0) return -1;
      if (availableB === 0 && availableA > 0) return 1;
      return deficitB - deficitA;
    });
  }

  getUrgencyLevel(product: Product): string {
    const available = product.quantity - product.reservedQty;
    if (available <= 0) return 'critical';
    if (available <= Math.floor(product.minQuantity / 2)) return 'high';
    return 'medium';
  }

  getUrgencyLabel(product: Product): string {
    const level = this.getUrgencyLevel(product);
    const labels: Record<string, string> = {
      critical: 'Critique',
      high: 'Élevée',
      medium: 'Moyenne'
    };
    return labels[level] || level;
  }

  getAvailableQty(product: Product): number {
    return product.quantity - product.reservedQty;
  }

  getDeficit(product: Product): number {
    return product.minQuantity - this.getAvailableQty(product);
  }
}
