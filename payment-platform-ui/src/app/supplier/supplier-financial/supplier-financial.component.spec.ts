// @ts-nocheck
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { RouterModule } from '@angular/router';
import { of, throwError } from 'rxjs';
import { AppIconComponent } from '../../components/icon/icon.component';
import { SupplierFinancialComponent } from './supplier-financial.component';
import { ReportService } from '../../services/report.service';
import { ToastService } from '../../services/toast.service';
import { ThemeService } from '../../services/theme.service';
import { LoginService } from '../../services/login.service';
import { BalanceService } from '../../services/balance.service';
import { TranslateStubPipe, translateServiceProvider } from '../../testing/translate-stubs';

function reportFixture(overrides: Record<string, unknown> = {}) {
  return {
    from: '2025-11-01',
    to: '2026-10-25',
    previousFrom: '2024-12-08',
    previousTo: '2025-10-31',
    granularity: 'MONTH',
    summary: {
      revenue: 12500,
      orderCount: 42,
      averageBasket: 297.62,
      openOrders: 8,
      deliveredOrders: 30
    },
    previousSummary: {
      revenue: 10000,
      orderCount: 35,
      averageBasket: 285.71,
      openOrders: 7,
      deliveredOrders: 24
    },
    series: [
      { bucket: '2026-08', orderCount: 10, revenue: 3000 },
      { bucket: '2026-09', orderCount: 14, revenue: 4200 },
      { bucket: '2026-10', orderCount: 18, revenue: 5300 }
    ],
    previousSeries: [
      { bucket: '2025-08', orderCount: 8, revenue: 2500 },
      { bucket: '2025-09', orderCount: 11, revenue: 3300 },
      { bucket: '2025-10', orderCount: 16, revenue: 4200 }
    ],
    orderCountByStatus: {
      CONFIRMED: 5,
      PREPARING: 2,
      DELIVERED: 25,
      CANCELLED: 1
    },
    topProducts: [
      { productName: 'Produit A', totalQuantity: 120, totalRevenue: 6000 },
      { productName: 'Produit B', totalQuantity: 80, totalRevenue: 4000 }
    ],
    paymentSummary: {
      confirmedTotal: 8000,
      confirmedCount: 10,
      pendingTotal: 1500,
      pendingCount: 3,
      rejectedTotal: 500,
      rejectedCount: 1
    },
    ...overrides
  };
}

describe('SupplierFinancialComponent', () => {
  let reportService: jasmine.SpyObj<ReportService>;
  let toast: jasmine.SpyObj<ToastService>;
  let loginService: jasmine.SpyObj<LoginService>;
  let balanceService: jasmine.SpyObj<BalanceService>;

  beforeEach(async () => {
    const reportSpy = jasmine.createSpyObj('ReportService', ['getSupplierFinancialReport']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const loginSpy = jasmine.createSpyObj('LoginService', ['getCurrentUser']);
    const balanceSpy = jasmine.createSpyObj('BalanceService', ['listBySupplier']);
    reportSpy.getSupplierFinancialReport.and.returnValue(of(reportFixture() as any));
    loginSpy.getCurrentUser.and.returnValue(null);
    balanceSpy.listBySupplier.and.returnValue(of([]));
    await TestBed.configureTestingModule({
      declarations: [SupplierFinancialComponent, TranslateStubPipe, AppIconComponent],
      imports: [RouterModule.forRoot([])],
      providers: [provideHttpClient(), translateServiceProvider(), { provide: ReportService, useValue: reportSpy }, { provide: ToastService, useValue: toastSpy }, { provide: ThemeService, useValue: { theme$: of('light') } }, { provide: LoginService, useValue: loginSpy }, { provide: BalanceService, useValue: balanceSpy }]
    }).compileComponents();
    reportService = TestBed.inject(ReportService) as jasmine.SpyObj<ReportService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
    balanceService = TestBed.inject(BalanceService) as jasmine.SpyObj<BalanceService>;
  });

  it('should create', () => {
    const fixture = TestBed.createComponent(SupplierFinancialComponent);
    const component = fixture.componentInstance;
    expect(component).toBeDefined();
  });

  it('should load the last 12 months by default', () => {
    const fixture = TestBed.createComponent(SupplierFinancialComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    const query = reportService.getSupplierFinancialReport.calls.mostRecent().args[0];
    expect(query.granularity).toBe('MONTH');
    expect(query.to).toMatch(/^\d{4}-\d{2}-\d{2}$/);
    expect(query.from).toMatch(/^\d{4}-\d{2}-01$/);
    expect(component.report?.summary.revenue).toBe(12500);
    expect(component.loading).toBeFalse();
  });

  it('should load a 30-day window when the day period is selected', () => {
    const fixture = TestBed.createComponent(SupplierFinancialComponent);
    const component = fixture.componentInstance;

    component.selectPeriod('DAY');

    const query = reportService.getSupplierFinancialReport.calls.mostRecent().args[0];
    expect(query.granularity).toBe('DAY');
    expect(query.from).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });

  it('should reject an invalid custom range without calling the API', () => {
    const fixture = TestBed.createComponent(SupplierFinancialComponent);
    const component = fixture.componentInstance;
    const callsBefore = reportService.getSupplierFinancialReport.calls.count();

    component.selectedPeriod = 'CUSTOM';
    component.customFrom = '2026-10-20';
    component.customTo = '2026-10-01';
    component.applyCustomRange();

    expect(component.rangeError).toBe('FINANCE360.INVALID_RANGE');
    expect(reportService.getSupplierFinancialReport.calls.count()).toBe(callsBefore);
  });

  it('should apply a valid custom range with its granularity', () => {
    const fixture = TestBed.createComponent(SupplierFinancialComponent);
    const component = fixture.componentInstance;

    component.selectedPeriod = 'CUSTOM';
    component.customFrom = '2026-01-01';
    component.customTo = '2026-03-31';
    component.customGranularity = 'WEEK';
    component.applyCustomRange();

    expect(reportService.getSupplierFinancialReport).toHaveBeenCalledWith({
      from: '2026-01-01',
      to: '2026-03-31',
      granularity: 'WEEK'
    });
  });

  it('should compute growth indicators from the previous period', () => {
    const fixture = TestBed.createComponent(SupplierFinancialComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.revenueGrowth).toBeCloseTo(25, 5);
    expect(component.ordersGrowth).toBeCloseTo(20, 5);
    expect(component.formatGrowth(null)).toBe('FINANCE360.NO_BASELINE');
  });

  it('should split payments into confirmed, pending and rejected segments', () => {
    const fixture = TestBed.createComponent(SupplierFinancialComponent);
    const component = fixture.componentInstance;
    fixture.detectChanges();

    expect(component.paymentTotal).toBe(10000);
    expect(component.paymentSegments.map((segment) => segment.percent)).toEqual([80, 15, 5]);
  });

  it('should set loadError to key on error without message', () => {
    reportService.getSupplierFinancialReport.and.returnValue(throwError(() => ({ error: {} })));
    const fixture = TestBed.createComponent(SupplierFinancialComponent);
    const component = fixture.componentInstance;
    component.loadReport();
    expect(component.loadError).toBe('FINANCE.LOAD_ERROR');
    expect(toast.error).toHaveBeenCalledWith('FINANCE.LOAD_ERROR');
  });

  it('should preserve server message on error', () => {
    reportService.getSupplierFinancialReport.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
    const fixture = TestBed.createComponent(SupplierFinancialComponent);
    const component = fixture.componentInstance;
    component.loadReport();
    expect(component.loadError).toBe('Err');
  });

  it('should load shop balances and compute outstanding totals', () => {
    loginService.getCurrentUser.and.returnValue({ organizationId: 'sup-1' } as any);
    balanceService.listBySupplier.and.returnValue(of([
      { shopId: 'b1', shopName: 'Boutique 1', currentBalance: 100, totalOrders: 2, totalPayments: 1, remainingDue: 150.5, lastTransaction: '2026-01-01' },
      { shopId: 'b2', shopName: 'Boutique 2', currentBalance: 0, totalOrders: 0, totalPayments: 0, remainingDue: 49.5, lastTransaction: '2026-01-02' }
    ] as any));
    const fixture = TestBed.createComponent(SupplierFinancialComponent);
    const component = fixture.componentInstance;
    component.loadReport();
    expect(balanceService.listBySupplier).toHaveBeenCalledWith('sup-1');
    expect(component.shopBalances.length).toBe(2);
    expect(component.activeShopCount).toBe(2);
    expect(component.totalDueByShops).toBe(200);
  });

  it('should degrade silently when balances fail (report unaffected)', () => {
    loginService.getCurrentUser.and.returnValue({ organizationId: 'sup-1' } as any);
    balanceService.listBySupplier.and.returnValue(throwError(() => new Error('boom')));
    const fixture = TestBed.createComponent(SupplierFinancialComponent);
    const component = fixture.componentInstance;
    component.loadReport();
    expect(component.shopBalances).toEqual([]);
    expect(component.loadError).toBeNull();
    expect(toast.error).not.toHaveBeenCalled();
  });

  it('should not query balances without organizationId', () => {
    loginService.getCurrentUser.and.returnValue(null);
    const fixture = TestBed.createComponent(SupplierFinancialComponent);
    const component = fixture.componentInstance;
    component.loadReport();
    expect(balanceService.listBySupplier).not.toHaveBeenCalled();
    expect(component.totalDueByShops).toBe(0);
    expect(component.activeShopCount).toBe(0);
  });
});
