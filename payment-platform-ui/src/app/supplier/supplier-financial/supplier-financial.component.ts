import { Component, OnDestroy, OnInit } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { Subscription } from 'rxjs';
import { ReportService, SupplierFinancialQuery } from '../../services/report.service';
import {
  PaymentSummary,
  ReportGranularity,
  ReportSummary,
  SupplierFinancialReport,
  TopProduct
} from '../../models/supplier-financial.model';
import { ToastService } from '../../services/toast.service';
import { LoginService } from '../../services/login.service';
import { BalanceService } from '../../services/balance.service';
import { BalanceSummary } from '../../models/balance.model';
import { statusLabelFr } from '../../pipes/status-label.pipe';

type PeriodMode = ReportGranularity | 'CUSTOM';

interface PeriodOption {
  key: PeriodMode;
  labelKey: string;
}

interface ChartPoint {
  bucket: string;
  label: string;
  fullLabel: string;
  revenue: number;
  orderCount: number;
  x: number;
  y: number;
  orderBarHeight: number;
  showLabel: boolean;
}

interface PreviousChartPoint {
  bucket: string;
  x: number;
  y: number;
}

interface PipelineEntry {
  status: string;
  label: string;
  count: number;
  percent: number;
  color: string;
}

interface PaymentSegment {
  labelKey: string;
  amount: number;
  count: number;
  percent: number;
  color: string;
}

interface ShopExposure {
  shop: BalanceSummary;
  width: number;
}

const EMPTY_SUMMARY: ReportSummary = {
  revenue: 0,
  orderCount: 0,
  averageBasket: 0,
  openOrders: 0,
  deliveredOrders: 0
};

const EMPTY_PAYMENTS: PaymentSummary = {
  confirmedTotal: 0,
  confirmedCount: 0,
  pendingTotal: 0,
  pendingCount: 0,
  rejectedTotal: 0,
  rejectedCount: 0
};

const CHART_WIDTH = 1000;
const CHART_HEIGHT = 320;
const CHART_PAD = { top: 24, right: 18, bottom: 38, left: 58 };
const STATUS_ORDER = [
  'DRAFT', 'CONFIRMED', 'PREPARING', 'READY_FOR_DELIVERY', 'DELIVERY_ACCEPTED',
  'IN_DELIVERY', 'DELIVERED', 'ACCEPTED', 'DELIVERY_REJECTED', 'CANCELLED', 'REJECTED'
];

@Component({
    selector: 'app-supplier-financial',
    templateUrl: './supplier-financial.component.html',
    styleUrls: ['./supplier-financial.component.css'],
    standalone: false
})
export class SupplierFinancialComponent implements OnInit, OnDestroy {
  report: SupplierFinancialReport | null = null;
  loading = true;
  loadError: string | null = null;
  shopBalances: BalanceSummary[] = [];
  balancesLoading = false;
  rangeError: string | null = null;
  updatedAt: Date | null = null;

  readonly periodOptions: PeriodOption[] = [
    { key: 'DAY', labelKey: 'FINANCE360.PERIOD_DAY' },
    { key: 'WEEK', labelKey: 'FINANCE360.PERIOD_WEEK' },
    { key: 'MONTH', labelKey: 'FINANCE360.PERIOD_MONTH' },
    { key: 'YEAR', labelKey: 'FINANCE360.PERIOD_YEAR' },
    { key: 'CUSTOM', labelKey: 'FINANCE360.PERIOD_CUSTOM' }
  ];
  readonly granularities: ReportGranularity[] = ['DAY', 'WEEK', 'MONTH', 'YEAR'];

  selectedPeriod: PeriodMode = 'MONTH';
  customFrom = '';
  customTo = '';
  customGranularity: ReportGranularity = 'DAY';

  private alive = true;
  private reportSubscription?: Subscription;

  constructor(
    private reportService: ReportService,
    private toast: ToastService,
    private translate: TranslateService,
    private loginService: LoginService,
    private balanceService: BalanceService
  ) {}

  ngOnInit(): void {
    this.initCustomRange();
    this.loadReport();
  }

  ngOnDestroy(): void {
    this.alive = false;
    this.reportSubscription?.unsubscribe();
  }

  get summary(): ReportSummary {
    return this.report?.summary ?? EMPTY_SUMMARY;
  }

  get previousSummary(): ReportSummary {
    return this.report?.previousSummary ?? EMPTY_SUMMARY;
  }

  get payments(): PaymentSummary {
    return this.report?.paymentSummary ?? EMPTY_PAYMENTS;
  }

  get totalDueByShops(): number {
    return this.shopBalances.reduce((sum, b) => sum + Number(b.remainingDue || 0), 0);
  }

  get activeShopCount(): number {
    return this.shopBalances.length;
  }

  get revenueGrowth(): number | null {
    return this.growth(this.summary.revenue, this.previousSummary.revenue);
  }

  get ordersGrowth(): number | null {
    return this.growth(this.summary.orderCount, this.previousSummary.orderCount);
  }

  get basketGrowth(): number | null {
    return this.growth(this.summary.averageBasket, this.previousSummary.averageBasket);
  }

  get paymentTotal(): number {
    const p = this.payments;
    return Number(p.confirmedTotal) + Number(p.pendingTotal) + Number(p.rejectedTotal);
  }

  get paymentSegments(): PaymentSegment[] {
    const total = this.paymentTotal;
    const p = this.payments;
    const percent = (value: number) => total > 0 ? (value / total) * 100 : 0;
    return [
      {
        labelKey: 'FINANCE360.CONFIRMED',
        amount: Number(p.confirmedTotal),
        count: p.confirmedCount,
        percent: percent(Number(p.confirmedTotal)),
        color: '#14b8a6'
      },
      {
        labelKey: 'FINANCE360.PENDING',
        amount: Number(p.pendingTotal),
        count: p.pendingCount,
        percent: percent(Number(p.pendingTotal)),
        color: '#f59e0b'
      },
      {
        labelKey: 'FINANCE360.REJECTED',
        amount: Number(p.rejectedTotal),
        count: p.rejectedCount,
        percent: percent(Number(p.rejectedTotal)),
        color: '#ef4444'
      }
    ];
  }

  get pipelineEntries(): PipelineEntry[] {
    const counts = this.report?.orderCountByStatus ?? {};
    const entries = Object.entries(counts)
      .filter(([, count]) => Number(count) > 0)
      .sort(([a], [b]) => this.statusRank(a) - this.statusRank(b));
    const total = entries.reduce((sum, [, count]) => sum + Number(count), 0);
    return entries.map(([status, count]) => ({
      status,
      label: this.statusLabel(status),
      count: Number(count),
      percent: total > 0 ? (Number(count) / total) * 100 : 0,
      color: this.statusColor(status)
    }));
  }

  get pipelineTotal(): number {
    return this.pipelineEntries.reduce((sum, entry) => sum + entry.count, 0);
  }

  get pipelineGradient(): string {
    const entries = this.pipelineEntries;
    if (entries.length === 0) {
      return 'conic-gradient(var(--border-light) 0 100%)';
    }
    let cursor = 0;
    const parts = entries.map((entry) => {
      const start = cursor;
      cursor += entry.percent;
      return `${entry.color} ${start.toFixed(2)}% ${cursor.toFixed(2)}%`;
    });
    return `conic-gradient(${parts.join(', ')})`;
  }

  get topProducts(): TopProduct[] {
    return (this.report?.topProducts ?? []).slice(0, 5);
  }

  get topProductMaxRevenue(): number {
    return Math.max(1, ...this.topProducts.map((p) => Number(p.totalRevenue)));
  }

  get topShops(): ShopExposure[] {
    const shops = [...this.shopBalances]
      .filter((shop) => Number(shop.remainingDue) > 0)
      .sort((a, b) => Number(b.remainingDue) - Number(a.remainingDue))
      .slice(0, 5);
    const max = Math.max(1, ...shops.map((shop) => Number(shop.remainingDue)));
    return shops.map((shop) => ({
      shop,
      width: Math.max(4, (Number(shop.remainingDue) / max) * 100)
    }));
  }

  get hasSeries(): boolean {
    return (this.report?.series?.length ?? 0) > 0;
  }

  get chartMaxRevenue(): number {
    const values = [
      ...(this.report?.series ?? []).map((point) => Number(point.revenue)),
      ...(this.report?.previousSeries ?? []).map((point) => Number(point.revenue))
    ];
    return Math.max(1, ...values);
  }

  get chartPoints(): ChartPoint[] {
    const series = this.report?.series ?? [];
    const points = this.sample(series, 60);
    const plotWidth = CHART_WIDTH - CHART_PAD.left - CHART_PAD.right;
    const plotHeight = CHART_HEIGHT - CHART_PAD.top - CHART_PAD.bottom;
    const maxRevenue = this.chartMaxRevenue;
    const maxOrders = Math.max(1, ...series.map((point) => Number(point.orderCount)));
    const showEvery = Math.max(1, Math.ceil(points.length / 8));

    return points.map(({ item, index }) => {
      const x = series.length <= 1
        ? CHART_PAD.left + plotWidth / 2
        : CHART_PAD.left + (index / (series.length - 1)) * plotWidth;
      const ratio = Number(item.revenue) / maxRevenue;
      const y = CHART_PAD.top + (1 - ratio) * plotHeight;
      return {
        bucket: item.bucket,
        label: this.formatBucket(item.bucket),
        fullLabel: this.formatBucket(item.bucket, true),
        revenue: Number(item.revenue),
        orderCount: Number(item.orderCount),
        x,
        y,
        orderBarHeight: (Number(item.orderCount) / maxOrders) * plotHeight * 0.26,
        showLabel: index === 0 || index === series.length - 1 || index % showEvery === 0
      };
    });
  }

  get previousChartPoints(): PreviousChartPoint[] {
    const series = this.report?.previousSeries ?? [];
    const points = this.sample(series, 60);
    const plotWidth = CHART_WIDTH - CHART_PAD.left - CHART_PAD.right;
    const plotHeight = CHART_HEIGHT - CHART_PAD.top - CHART_PAD.bottom;
    return points.map(({ item, index }) => ({
      bucket: item.bucket,
      x: series.length <= 1
        ? CHART_PAD.left + plotWidth / 2
        : CHART_PAD.left + (index / (series.length - 1)) * plotWidth,
      y: CHART_PAD.top + (1 - Number(item.revenue) / this.chartMaxRevenue) * plotHeight
    }));
  }

  get linePath(): string {
    return this.smoothPath(this.chartPoints.map((point) => ({ x: point.x, y: point.y })));
  }

  get areaPath(): string {
    const points = this.chartPoints;
    if (points.length === 0) return '';
    const baseline = CHART_HEIGHT - CHART_PAD.bottom;
    return `${this.smoothPath(points)} L ${points[points.length - 1].x.toFixed(2)} ${baseline} `
      + `L ${points[0].x.toFixed(2)} ${baseline} Z`;
  }

  get previousPath(): string {
    return this.smoothPath(this.previousChartPoints);
  }

  get chartGrid(): Array<{ y: number; label: string }> {
    const plotHeight = CHART_HEIGHT - CHART_PAD.top - CHART_PAD.bottom;
    return [0, 0.25, 0.5, 0.75, 1].map((ratio) => ({
      y: CHART_PAD.top + ratio * plotHeight,
      label: this.formatCompact(this.chartMaxRevenue * (1 - ratio))
    }));
  }

  get miniPath(): string {
    return this.smoothPath(this.miniPoints);
  }

  get miniAreaPath(): string {
    const points = this.miniPoints;
    if (points.length === 0) return '';
    return `${this.smoothPath(points)} L ${points[points.length - 1].x} 66 L ${points[0].x} 66 Z`;
  }

  get miniPoints(): Array<{ x: number; y: number }> {
    const series = this.sample(this.report?.series ?? [], 24);
    const max = this.chartMaxRevenue;
    return series.map(({ item, index }) => ({
      x: 4 + (index / Math.max(1, (this.report?.series?.length ?? 1) - 1)) * 232,
      y: 62 - (Number(item.revenue) / max) * 54
    }));
  }

  get rangeLabel(): string {
    if (!this.report) return '';
    return `${this.formatDate(this.report.from)} → ${this.formatDate(this.report.to)}`;
  }

  get comparisonLabel(): string {
    if (!this.report) return '';
    return `${this.formatDate(this.report.previousFrom)} → ${this.formatDate(this.report.previousTo)}`;
  }

  loadReport(): void {
    const query = this.buildQuery();
    if (!query) return;

    const hadReport = !!this.report;
    this.loading = true;
    this.loadError = null;
    this.loadShopBalances();

    this.reportSubscription?.unsubscribe();
    this.reportSubscription = this.reportService.getSupplierFinancialReport(query).subscribe({
      next: (data) => {
        if (!this.alive) return;
        this.report = data;
        this.updatedAt = new Date();
        this.loading = false;
      },
      error: (err: { error?: { message?: string } }) => {
        if (!this.alive) return;
        const message = err.error?.message || this.translate.instant('FINANCE.LOAD_ERROR');
        this.loadError = message;
        this.loading = false;
        if (!hadReport) {
          this.toast.error(message);
        }
      }
    });
  }

  selectPeriod(mode: PeriodMode): void {
    this.selectedPeriod = mode;
    this.rangeError = null;
    if (mode === 'CUSTOM') {
      if (!this.customFrom || !this.customTo) {
        this.initCustomRange();
      }
      return;
    }
    this.loadReport();
  }

  onCustomGranularityChange(event: Event): void {
    const value = (event.target as HTMLSelectElement).value as ReportGranularity;
    if (this.granularities.includes(value)) {
      this.customGranularity = value;
    }
  }

  applyCustomRange(): void {
    this.rangeError = null;
    if (!this.customFrom || !this.customTo) {
      this.rangeError = this.translate.instant('FINANCE360.INVALID_RANGE');
      return;
    }
    if (this.customFrom > this.customTo) {
      this.rangeError = this.translate.instant('FINANCE360.INVALID_RANGE');
      return;
    }
    this.loadReport();
  }

  refresh(): void {
    this.loadReport();
  }

  growth(current: number, previous: number): number | null {
    if (previous === 0) {
      return current === 0 ? 0 : null;
    }
    return ((current - previous) / previous) * 100;
  }

  growthClass(value: number | null): string {
    if (value === null) return 'neutral';
    if (value > 0.05) return 'positive';
    if (value < -0.05) return 'negative';
    return 'neutral';
  }

  formatGrowth(value: number | null): string {
    if (value === null) return this.translate.instant('FINANCE360.NO_BASELINE');
    const arrow = value > 0.05 ? '↑' : value < -0.05 ? '↓' : '→';
    return `${arrow} ${value >= 0 ? '+' : ''}${this.formatNumber(value, 1)}%`;
  }

  formatMoney(value: number | null | undefined): string {
    return `${this.formatNumber(Number(value || 0), 2)} TND`;
  }

  formatNumber(value: number, digits = 0): string {
    return new Intl.NumberFormat(this.currentLang(), {
      minimumFractionDigits: digits,
      maximumFractionDigits: digits
    }).format(Number.isFinite(value) ? value : 0);
  }

  formatCompact(value: number): string {
    const abs = Math.abs(value);
    if (abs >= 1_000_000) return `${this.formatNumber(value / 1_000_000, 1)} M`;
    if (abs >= 1_000) return `${this.formatNumber(value / 1_000, 1)} k`;
    return this.formatNumber(value, 0);
  }

  formatDate(iso: string | null | undefined): string {
    if (!iso) return '—';
    const date = this.parseDate(iso);
    return date.toLocaleDateString(this.currentLang(), {
      day: '2-digit',
      month: 'short',
      year: 'numeric'
    });
  }

  formatBucket(bucket: string, full = false): string {
    if (!bucket) return '';
    if (this.report?.granularity === 'YEAR') return bucket;
    if (this.report?.granularity === 'MONTH') {
      const [year, month] = bucket.split('-').map(Number);
      return new Date(year, month - 1, 1).toLocaleDateString(this.currentLang(), full
        ? { month: 'long', year: 'numeric' }
        : { month: 'short', year: '2-digit' });
    }
    const date = this.parseDate(bucket);
    return date.toLocaleDateString(this.currentLang(), full
      ? { day: '2-digit', month: 'long', year: 'numeric' }
      : { day: '2-digit', month: 'short' });
  }

  formatUpdatedAt(): string {
    if (!this.updatedAt) return '';
    return this.updatedAt.toLocaleTimeString(this.currentLang(), {
      hour: '2-digit',
      minute: '2-digit'
    });
  }

  productWidth(product: TopProduct): number {
    return Math.max(4, (Number(product.totalRevenue) / this.topProductMaxRevenue) * 100);
  }

  statusLabel(status: string): string {
    const key = `STATUS.${status}`;
    const translated = this.translate.instant(key);
    return translated && translated !== key ? translated : statusLabelFr(status);
  }

  statusColor(status: string): string {
    const colors: Record<string, string> = {
      DRAFT: '#94a3b8',
      CONFIRMED: '#38bdf8',
      PREPARING: '#f59e0b',
      READY_FOR_DELIVERY: '#8b5cf6',
      DELIVERY_ACCEPTED: '#14b8a6',
      DELIVERY_REJECTED: '#ef4444',
      IN_DELIVERY: '#6366f1',
      DELIVERED: '#22c55e',
      ACCEPTED: '#0d9488',
      CANCELLED: '#cbd5e1',
      REJECTED: '#ef4444'
    };
    return colors[status] || '#94a3b8';
  }

  private buildQuery(): SupplierFinancialQuery | null {
    if (this.selectedPeriod === 'CUSTOM') {
      if (!this.customFrom || !this.customTo || this.customFrom > this.customTo) {
        this.rangeError = this.translate.instant('FINANCE360.INVALID_RANGE');
        return null;
      }
      return {
        from: this.customFrom,
        to: this.customTo,
        granularity: this.customGranularity
      };
    }

    const today = this.startOfDay(new Date());
    switch (this.selectedPeriod) {
      case 'DAY':
        return {
          from: this.toIsoDate(this.addDays(today, -29)),
          to: this.toIsoDate(today),
          granularity: 'DAY'
        };
      case 'WEEK':
        return {
          from: this.toIsoDate(this.addDays(this.startOfWeek(today), -77)),
          to: this.toIsoDate(today),
          granularity: 'WEEK'
        };
      case 'YEAR':
        return {
          from: `${today.getFullYear() - 4}-01-01`,
          to: this.toIsoDate(today),
          granularity: 'YEAR'
        };
      case 'MONTH':
      default:
        return {
          from: this.toIsoDate(new Date(today.getFullYear(), today.getMonth() - 11, 1)),
          to: this.toIsoDate(today),
          granularity: 'MONTH'
        };
    }
  }

  private loadShopBalances(): void {
    const supplierId = this.loginService.getCurrentUser()?.organizationId;
    if (!supplierId) {
      this.shopBalances = [];
      return;
    }
    this.balancesLoading = true;
    this.balanceService.listBySupplier(String(supplierId)).subscribe({
      next: (balances) => {
        if (!this.alive) return;
        this.shopBalances = balances || [];
        this.balancesLoading = false;
      },
      error: () => {
        if (!this.alive) return;
        this.shopBalances = [];
        this.balancesLoading = false;
      }
    });
  }

  private initCustomRange(): void {
    const today = this.startOfDay(new Date());
    this.customFrom = this.toIsoDate(this.addDays(today, -29));
    this.customTo = this.toIsoDate(today);
    this.customGranularity = 'DAY';
  }

  private sample<T>(items: T[], max: number): Array<{ item: T; index: number }> {
    if (items.length <= max) {
      return items.map((item, index) => ({ item, index }));
    }
    const step = (items.length - 1) / (max - 1);
    return Array.from({ length: max }, (_, i) => {
      const index = Math.round(i * step);
      return { item: items[index], index };
    });
  }

  private smoothPath(points: Array<{ x: number; y: number }>): string {
    if (points.length === 0) return '';
    if (points.length === 1) {
      return `M ${points[0].x.toFixed(2)} ${points[0].y.toFixed(2)}`;
    }
    let path = `M ${points[0].x.toFixed(2)} ${points[0].y.toFixed(2)}`;
    for (let i = 1; i < points.length; i++) {
      const previous = points[i - 1];
      const current = points[i];
      const middle = (previous.x + current.x) / 2;
      path += ` C ${middle.toFixed(2)} ${previous.y.toFixed(2)}, `
        + `${middle.toFixed(2)} ${current.y.toFixed(2)}, `
        + `${current.x.toFixed(2)} ${current.y.toFixed(2)}`;
    }
    return path;
  }

  private statusRank(status: string): number {
    const index = STATUS_ORDER.indexOf(status);
    return index >= 0 ? index : STATUS_ORDER.length;
  }

  private currentLang(): string {
    return this.translate.currentLang || this.translate.getDefaultLang?.() || 'fr';
  }

  private parseDate(value: string): Date {
    const [year, month, day] = value.split('-').map(Number);
    if (day) return new Date(year, month - 1, day);
    return new Date(year, month - 1, 1);
  }

  private startOfDay(date: Date): Date {
    return new Date(date.getFullYear(), date.getMonth(), date.getDate());
  }

  private startOfWeek(date: Date): Date {
    const day = date.getDay();
    const diff = day === 0 ? -6 : 1 - day;
    return this.addDays(date, diff);
  }

  private addDays(date: Date, days: number): Date {
    const copy = new Date(date);
    copy.setDate(copy.getDate() + days);
    return copy;
  }

  private toIsoDate(date: Date): string {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    return `${year}-${month}-${day}`;
  }
}
