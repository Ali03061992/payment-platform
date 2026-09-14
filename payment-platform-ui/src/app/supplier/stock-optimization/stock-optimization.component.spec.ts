import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { StockOptimizationComponent } from './stock-optimization.component';
import { StockOptimizationService } from '../../services/stock-optimization.service';
import { ToastService } from '../../services/toast.service';
import { StockOptimizationResponse, ProductOptimization } from '../../models/stock-optimization.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('StockOptimizationComponent', () => {
  let component: StockOptimizationComponent;
  let fixture: ComponentFixture<StockOptimizationComponent>;
  let optService: jasmine.SpyObj<StockOptimizationService>;
  let toast: jasmine.SpyObj<ToastService>;

  const mockResponse: StockOptimizationResponse = {
    products: [], abcDistribution: {}, xyzDistribution: {}, abcxzDistribution: {},
    actionDistribution: {}, riskDistribution: {}, totalStockValue: 1000,
    totalSafetyStockValue: 200, productsToOrder: 5, productsAtRisk: 3,
    productsOverstocked: 2, summary: 'Summary text', calculatedAt: ''
  };

  beforeEach(() => {
    const optSpy = jasmine.createSpyObj('StockOptimizationService', ['optimize', 'configure']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    optSpy.optimize.and.returnValue(of(mockResponse));

    TestBed.configureTestingModule({
    declarations: [StockOptimizationComponent],
    imports: [FormsModule],
    providers: [
        { provide: StockOptimizationService, useValue: optSpy },
        { provide: ToastService, useValue: toastSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(StockOptimizationComponent);
    component = fixture.componentInstance;
    optService = TestBed.inject(StockOptimizationService) as jasmine.SpyObj<StockOptimizationService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('loadOptimization', () => {
    it('should load optimization data', () => {
      component.loadOptimization();
      expect(component.data).toBeTruthy();
      expect(component.loading).toBeFalse();
    });

    it('should handle error', () => {
      optService.optimize.and.returnValue(throwError(() => ({ error: { message: 'Load error' } })));
      component.loadOptimization();
      expect(component.error).toBe('Load error');
      expect(component.loading).toBeFalse();
    });
  });

  describe('applyConfig', () => {
    it('should apply config and show success', () => {
      optService.configure.and.returnValue(of(mockResponse));
      component.applyConfig();
      expect(component.showConfig).toBeFalse();
      expect(toast.success).toHaveBeenCalled();
    });

    it('should handle config error', () => {
      optService.configure.and.returnValue(throwError(() => ({ error: { message: 'Config error' } })));
      component.applyConfig();
      expect(toast.error).toHaveBeenCalled();
    });
  });

  describe('selectProduct', () => {
    it('should select product', () => {
      const p = { productId: 1 } as ProductOptimization;
      component.selectProduct(p);
      expect(component.selectedProduct).toBe(p);
    });

    it('should deselect if same product', () => {
      const p = { productId: 1 } as ProductOptimization;
      component.selectedProduct = p;
      component.selectProduct(p);
      expect(component.selectedProduct).toBeNull();
    });
  });

  describe('abcColor', () => {
    it('should return correct colors', () => {
      expect(component.abcColor('A')).toBe('#c62828');
      expect(component.abcColor('B')).toBe('#e65100');
      expect(component.abcColor('C')).toBe('#2e7d32');
      expect(component.abcColor('X')).toBe('#666');
    });
  });

  describe('abcBg', () => {
    it('should return correct bg colors', () => {
      expect(component.abcBg('A')).toBe('#fce4ec');
      expect(component.abcBg('B')).toBe('#fff3e0');
      expect(component.abcBg('C')).toBe('#e8f5e9');
    });
  });

  describe('xyzColor', () => {
    it('should return correct colors', () => {
      expect(component.xyzColor('X')).toBe('#2e7d32');
      expect(component.xyzColor('Y')).toBe('#e65100');
      expect(component.xyzColor('Z')).toBe('#c62828');
    });
  });

  describe('actionColor', () => {
    it('should return correct colors', () => {
      expect(component.actionColor('ORDER_NOW')).toBe('#c62828');
      expect(component.actionColor('MONITOR')).toBe('#f9a825');
      expect(component.actionColor('UNKNOWN')).toBe('#666');
    });
  });

  describe('riskColor', () => {
    it('should return correct colors', () => {
      expect(component.riskColor('CRITICAL')).toBe('#c62828');
      expect(component.riskColor('HIGH')).toBe('#e65100');
      expect(component.riskColor('MEDIUM')).toBe('#f9a825');
      expect(component.riskColor('LOW')).toBe('#2e7d32');
    });
  });

  describe('formatPercent', () => {
    it('should format percent', () => {
      expect(component.formatPercent(0.5)).toBe('50.0%');
      expect(component.formatPercent(0.123)).toBe('12.3%');
    });
  });

  describe('formatNumber', () => {
    it('should format number', () => {
      expect(component.formatNumber(5.67)).toBe('5.7');
    });
  });
});
