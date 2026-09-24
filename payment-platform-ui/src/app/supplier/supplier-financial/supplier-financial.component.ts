import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit } from '@angular/core';
import { ReportService } from '../../services/report.service';
import { SupplierFinancialReport } from '../../models/supplier-financial.model';
import { ToastService } from '../../services/toast.service';
import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

@Component({
    selector: 'app-supplier-financial',
    templateUrl: './supplier-financial.component.html',
    styleUrls: ['./supplier-financial.component.css'],
    standalone: false
})
export class SupplierFinancialComponent implements OnInit, AfterViewInit, OnDestroy {
  report: SupplierFinancialReport | null = null;
  loading = true;

  @ViewChild('revenueChart') revenueChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('statusChart') statusChartRef!: ElementRef<HTMLCanvasElement>;
  @ViewChild('productsChart') productsChartRef!: ElementRef<HTMLCanvasElement>;

  private revenueChart: Chart | null = null;
  private statusChart: Chart | null = null;
  private productsChart: Chart | null = null;

  constructor(
    private reportService: ReportService,
    private toast: ToastService
  ) {}

  ngOnInit(): void {
    this.loadReport();
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.destroyCharts();
  }

  loadReport(): void {
    this.loading = true;
    this.reportService.getSupplierFinancialReport().subscribe({
      next: (data) => {
        this.report = data;
        this.loading = false;
        setTimeout(() => this.renderCharts(), 100);
      },
      error: (err: any) => {
        this.toast.error(err.error?.message || 'Erreur de chargement du rapport');
        this.loading = false;
      }
    });
  }

  private destroyCharts(): void {
    if (this.revenueChart) { this.revenueChart.destroy(); this.revenueChart = null; }
    if (this.statusChart) { this.statusChart.destroy(); this.statusChart = null; }
    if (this.productsChart) { this.productsChart.destroy(); this.productsChart = null; }
  }

  private renderCharts(): void {
    if (!this.report) return;
    this.destroyCharts();
    this.renderRevenueChart();
    this.renderStatusChart();
    this.renderProductsChart();
  }

  private renderRevenueChart(): void {
    if (!this.revenueChartRef || !this.report?.monthlyRevenue?.length) return;
    const labels = this.report.monthlyRevenue.map(m => m.month);
    const data = this.report.monthlyRevenue.map(m => m.revenue);

    this.revenueChart = new Chart(this.revenueChartRef.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: 'Chiffre d\'affaires (TND)',
          data,
          borderColor: '#1976d2',
          backgroundColor: 'rgba(25, 118, 210, 0.1)',
          fill: true,
          tension: 0.4,
          pointRadius: 4,
          pointBackgroundColor: '#1976d2'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: true, position: 'top' },
          tooltip: {
            callbacks: {
              label: (ctx) => `${(ctx.parsed.y ?? 0).toFixed(2)} TND`
            }
          }
        },
        scales: {
          y: {
            beginAtZero: true,
            ticks: { callback: (v) => `${v} TND` }
          }
        }
      }
    });
  }

  private renderStatusChart(): void {
    if (!this.statusChartRef || !this.report?.orderCountByStatus) return;
    const entries = Object.entries(this.report.orderCountByStatus);
    const labels = entries.map(([k]) => this.statusLabel(k));
    const data = entries.map(([, v]) => v);
    const colors = entries.map(([k]) => this.statusColor(k));

    this.statusChart = new Chart(this.statusChartRef.nativeElement, {
      type: 'doughnut',
      data: {
        labels,
        datasets: [{
          data,
          backgroundColor: colors,
          borderWidth: 2,
          borderColor: '#fff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'right', labels: { padding: 12 } }
        }
      }
    });
  }

  private renderProductsChart(): void {
    if (!this.productsChartRef || !this.report?.topProducts?.length) return;
    const labels = this.report.topProducts.map(p => p.productName);
    const data = this.report.topProducts.map(p => p.totalQuantity);

    this.productsChart = new Chart(this.productsChartRef.nativeElement, {
      type: 'bar',
      data: {
        labels,
        datasets: [{
          label: 'Quantité vendue',
          data,
          backgroundColor: 'rgba(79, 195, 247, 0.7)',
          borderColor: '#4fc3f7',
          borderWidth: 1
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y',
        plugins: {
          legend: { display: false }
        },
        scales: {
          x: { beginAtZero: true }
        }
      }
    });
  }

  statusLabel(s: string): string {
    const map: Record<string, string> = {
      DRAFT: 'Brouillon', CONFIRMED: 'Confirmé', PREPARING: 'En préparation',
      READY_FOR_DELIVERY: 'Prêt livraison', DELIVERY_ACCEPTED: 'Livraison acceptée',
      DELIVERY_REJECTED: 'Livraison rejetée', IN_DELIVERY: 'En livraison',
      DELIVERED: 'Livré', ACCEPTED: 'Accepté', CANCELLED: 'Annulé', REJECTED: 'Rejeté'
    };
    return map[s] || s;
  }

  objectKeys(obj: Record<string, number>): string[] {
    return Object.keys(obj);
  }

  statusColor(s: string): string {
    const map: Record<string, string> = {
      DRAFT: '#b0bec5', CONFIRMED: '#42a5f5', PREPARING: '#ffa726',
      READY_FOR_DELIVERY: '#ab47bc', DELIVERY_ACCEPTED: '#26a69a',
      DELIVERY_REJECTED: '#ef5350', IN_DELIVERY: '#5c6bc0',
      DELIVERED: '#66bb6a', ACCEPTED: '#00897b', CANCELLED: '#ef5350', REJECTED: '#e53935'
    };
    return map[s] || '#90a4ae';
  }
}
