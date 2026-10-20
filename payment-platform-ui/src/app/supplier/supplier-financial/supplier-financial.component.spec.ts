// @ts-nocheck
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { SupplierFinancialComponent } from './supplier-financial.component';
import { ReportService } from '../../services/report.service';
import { ToastService } from '../../services/toast.service';
import { ThemeService } from '../../services/theme.service';
import { LoginService } from '../../services/login.service';
import { BalanceService } from '../../services/balance.service';
import { TranslateStubPipe, translateServiceProvider } from '../../testing/translate-stubs';

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
    reportSpy.getSupplierFinancialReport.and.returnValue(of(null as any));
    loginSpy.getCurrentUser.and.returnValue(null);
    balanceSpy.listBySupplier.and.returnValue(of([]));
    await TestBed.configureTestingModule({
      declarations: [SupplierFinancialComponent, TranslateStubPipe],
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
