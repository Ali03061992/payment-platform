// @ts-nocheck
import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { of, throwError } from 'rxjs';
import { SupplierFinancialComponent } from './supplier-financial.component';
import { ReportService } from '../../services/report.service';
import { ToastService } from '../../services/toast.service';
import { ThemeService } from '../../services/theme.service';
import { TranslateStubPipe, translateServiceProvider } from '../../testing/translate-stubs';

describe('SupplierFinancialComponent', () => {
  let reportService: jasmine.SpyObj<ReportService>;
  let toast: jasmine.SpyObj<ToastService>;

  beforeEach(async () => {
    const reportSpy = jasmine.createSpyObj('ReportService', ['getSupplierFinancialReport']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    reportSpy.getSupplierFinancialReport.and.returnValue(of(null as any));
    await TestBed.configureTestingModule({
      declarations: [SupplierFinancialComponent, TranslateStubPipe],
      providers: [provideHttpClient(), translateServiceProvider(), { provide: ReportService, useValue: reportSpy }, { provide: ToastService, useValue: toastSpy }, { provide: ThemeService, useValue: { theme$: of('light') } }]
    }).compileComponents();
    reportService = TestBed.inject(ReportService) as jasmine.SpyObj<ReportService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
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
});
