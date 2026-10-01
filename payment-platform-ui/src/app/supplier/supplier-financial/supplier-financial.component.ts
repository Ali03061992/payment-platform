import { Component, OnInit, OnDestroy, ViewChild, ElementRef, AfterViewInit, ChangeDetectorRef } from '@angular/core';
import { ReportService } from '../../services/report.service';
import { SupplierFinancialReport } from '../../models/supplier-financial.model';
import { ToastService } from '../../services/toast.service';
import { ThemeService } from '../../services/theme.service';
import { Subscription } from 'rxjs';
import type { ECharts, EChartsCoreOption } from 'echarts/core';

@Component({
    selector: 'app-supplier-financial',
    templateUrl: './supplier-financial.component.html',
    styleUrls: ['./supplier-financial.component.css'],
    standalone: false
})
export class SupplierFinancialComponent implements OnInit, AfterViewInit, OnDestroy {
  report: SupplierFinancialReport | null = null;
  loading = true;
  loadError: string | null = null;

  @ViewChild('revenueChart') revenueChartRef!: ElementRef<HTMLDivElement>;
  @ViewChild('statusChart') statusChartRef!: ElementRef<HTMLDivElement>;
  @ViewChild('productsChart') productsChartRef!: ElementRef<HTMLDivElement>;

  private revenueChart: ECharts | null = null;
  private statusChart: ECharts | null = null;
  private productsChart: ECharts | null = null;
  private alive = true;
  private resizeListenerAttached = false;
  private themeSub: Subscription | null = null;
  /** ECharts chargé en lazy (hors bundle initial) au premier rendu. */
  private echartsPromise: Promise<typeof import('echarts/core')> | null = null;

  private loadEcharts(): Promise<typeof import('echarts/core')> {
    if (!this.echartsPromise) {
      this.echartsPromise = (async () => {
        const [core, charts, components, renderers] = await Promise.all([
          import('echarts/core'),
          import('echarts/charts'),
          import('echarts/components'),
          import('echarts/renderers'),
        ]);
        core.use([
          charts.LineChart, charts.PieChart, charts.BarChart,
          components.GridComponent, components.TooltipComponent, components.LegendComponent,
          renderers.SVGRenderer,
        ]);
        return core;
      })();
    }
    return this.echartsPromise;
  }

  constructor(
    private reportService: ReportService,
    private toast: ToastService,
    private cdr: ChangeDetectorRef,
    private themeService: ThemeService
  ) {}

  ngOnInit(): void {
    this.loadReport();
    // Re-rend les charts au changement de thème (couleurs lisibles clair/sombre).
    this.themeSub = this.themeService.theme$.subscribe(() => {
      if (this.alive && this.report && !this.loading) {
        this.renderCharts();
      }
    });
  }

  ngAfterViewInit(): void {}

  ngOnDestroy(): void {
    this.alive = false;
    this.themeSub?.unsubscribe();
    this.themeSub = null;
    if (this.resizeListenerAttached) {
      window.removeEventListener('resize', this.onWindowResize);
      this.resizeListenerAttached = false;
    }
    this.destroyCharts();
  }

  loadReport(): void {
    this.loading = true;
    this.loadError = null;
    console.log('[Financial] Chargement du rapport…');
    this.reportService.getSupplierFinancialReport().subscribe({
      next: (data) => {
        if (!this.alive) return;
        console.log('[Financial] Rapport reçu', {
          monthlyPoints: data?.monthlyRevenue?.length ?? 0,
          months: (data?.monthlyRevenue ?? []).map((m) => `${m.month}:${m.orderCount}/${m.revenue}`),
          orderStatuses: data?.orderCountByStatus ?? {},
          topProducts: data?.topProducts?.length ?? 0,
          paymentSummary: data?.paymentSummary ?? null,
        });
        this.report = data;
        this.loading = false;
        // Rendu déterministe : on force la détection de changements pour que les
        // <canvas> des blocs @if existent AVANT de créer les charts (fini le
        // setTimeout(100) qui ratait sa cible sur appareil lent).
        this.cdr.detectChanges();
        this.renderCharts();
      },
      error: (err: any) => {
        if (!this.alive) return;
        console.error('[Financial] Échec du chargement', {
          status: err?.status,
          statusText: err?.statusText,
          message: err?.error?.message,
          url: err?.url,
        });
        const message: string = err.error?.message || 'Erreur de chargement du rapport';
        this.loadError = message;
        this.toast.error(message);
        this.loading = false;
      }
    });
  }

  private destroyCharts(): void {
    if (this.revenueChart) { this.revenueChart.dispose(); this.revenueChart = null; }
    if (this.statusChart) { this.statusChart.dispose(); this.statusChart = null; }
    if (this.productsChart) { this.productsChart.dispose(); this.productsChart = null; }
  }

  private onWindowResize = (): void => {
    this.revenueChart?.resize();
    this.statusChart?.resize();
    this.productsChart?.resize();
  };

  private trackResize(): void {
    if (!this.resizeListenerAttached) {
      window.addEventListener('resize', this.onWindowResize);
      this.resizeListenerAttached = true;
    }
  }

  /** Couleurs lues depuis les variables CSS du thème actif (clair/sombre). */
  private themeColors(): { text: string; muted: string; border: string; card: string } {
    const css = getComputedStyle(document.documentElement);
    const pick = (name: string, fallback: string) => {
      const v = css.getPropertyValue(name).trim();
      return v || fallback;
    };
    return {
      text: pick('--text-primary', '#1a1a2e'),
      muted: pick('--text-muted', '#8896ab'),
      border: pick('--border-color', '#e8ecf1'),
      card: pick('--card-bg', '#ffffff'),
    };
  }

  /** Tooltip commune : confinée (mobile), fond carte du thème, texte lisible. */
  private tooltipBase(t: { card: string; border: string; text: string }): Record<string, unknown> {
    return {
      confine: true,
      backgroundColor: t.card,
      borderColor: t.border,
      borderWidth: 1,
      textStyle: { color: t.text, fontSize: 13 },
    };
  }

  private renderCharts(): void {
    if (!this.report) return;
    // Chaque chart est isolé : un canvas manquant ou une donnée vérolée ne doit
    // jamais empêcher les autres charts (ni boucler : un seul passage, pas de retry).
    console.log('[Financial] renderCharts refs', {
      revenue: !!this.revenueChartRef,
      status: !!this.statusChartRef,
      products: !!this.productsChartRef,
    });
    this.destroyCharts();
    this.trackResize();
    // ECharts est chargé en lazy (hors bundle initial) : un seul passage, pas de retry.
    this.loadEcharts().then((echarts) => {
      if (!this.alive || !this.report) return;
      try {
        this.renderRevenueChart(echarts);
        const el = this.revenueChartRef?.nativeElement;
        console.log('[Financial] revenue chart ok', { w: el?.clientWidth, h: el?.clientHeight });
      } catch (e) {
        console.error('[Financial] revenue chart FAILED', e);
      }
      try {
        this.renderStatusChart(echarts);
        const el = this.statusChartRef?.nativeElement;
        console.log('[Financial] status chart ok', { w: el?.clientWidth, h: el?.clientHeight });
      } catch (e) {
        console.error('[Financial] status chart FAILED', e);
      }
      try {
        this.renderProductsChart(echarts);
        const el = this.productsChartRef?.nativeElement;
        console.log('[Financial] products chart ok', { w: el?.clientWidth, h: el?.clientHeight });
      } catch (e) {
        console.error('[Financial] products chart FAILED', e);
      }
    });
  }

  private renderRevenueChart(echarts: typeof import('echarts/core')): void {
    if (!this.revenueChartRef || !this.report?.monthlyRevenue?.length) return;
    const labels = this.report.monthlyRevenue.map(m => m.month);
    const data = this.report.monthlyRevenue.map(m => Number(m.revenue));

    const option: EChartsCoreOption = {
      tooltip: {
        trigger: 'axis',
        valueFormatter: (v) => `${Number(v ?? 0).toFixed(2)} TND`,
      },
      grid: { left: 8, right: 16, top: 32, bottom: 8, containLabel: true },
      xAxis: { type: 'category', data: labels },
      yAxis: { type: 'value', axisLabel: { formatter: '{value} TND' } },
      series: [{
        name: "Chiffre d'affaires (TND)",
        type: 'line',
        smooth: true,
        data,
        lineStyle: { color: '#1976d2' },
        itemStyle: { color: '#1976d2' },
        areaStyle: { opacity: 0.1, color: '#1976d2' },
      }],
    };
    this.revenueChart = echarts.init(this.revenueChartRef.nativeElement, null, { renderer: 'svg' });
    this.revenueChart.setOption(option);
  }

  private renderStatusChart(echarts: typeof import('echarts/core')): void {
    if (!this.statusChartRef || !this.report?.orderCountByStatus) return;
    const entries = Object.entries(this.report.orderCountByStatus);

    const option: EChartsCoreOption = {
      tooltip: { trigger: 'item' },
      // Pas de légende latérale (chevauchement) : chaque part porte déjà son label.
      legend: { show: false },
      series: [{
        type: 'pie',
        radius: ['45%', '70%'],
        itemStyle: { borderColor: '#fff', borderWidth: 2 },
        label: { formatter: '{b}: {c}' },
        data: entries.map(([k, v]) => ({ name: this.statusLabel(k), value: v })),
        color: entries.map(([k]) => this.statusColor(k)),
      }],
    };
    this.statusChart = echarts.init(this.statusChartRef.nativeElement, null, { renderer: 'svg' });
    this.statusChart.setOption(option);
  }

  private renderProductsChart(echarts: typeof import('echarts/core')): void {
    if (!this.productsChartRef || !this.report?.topProducts?.length) return;
    const labels = this.report.topProducts.map(p => p.productName);
    const data = this.report.topProducts.map(p => Number(p.totalQuantity));

    const option: EChartsCoreOption = {
      tooltip: { trigger: 'axis' },
      grid: { left: 8, right: 16, top: 16, bottom: 8, containLabel: true },
      xAxis: { type: 'value' },
      yAxis: { type: 'category', data: labels },
      series: [{
        name: 'Quantité vendue',
        type: 'bar',
        data,
        itemStyle: { color: '#4fc3f7' },
      }],
    };
    this.productsChart = echarts.init(this.productsChartRef.nativeElement, null, { renderer: 'svg' });
    this.productsChart.setOption(option);
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
