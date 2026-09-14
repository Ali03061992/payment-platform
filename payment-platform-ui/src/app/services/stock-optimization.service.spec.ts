import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { StockOptimizationService } from './stock-optimization.service';
import { StockOptimizationResponse } from '../models/stock-optimization.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('StockOptimizationService', () => {
  let service: StockOptimizationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    sessionStorage.setItem('user', JSON.stringify({ organizationId: 1 }));
    TestBed.configureTestingModule({
    imports: [],
    providers: [StockOptimizationService, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
});
    service = TestBed.inject(StockOptimizationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('optimize', () => {
    it('should GET optimization data', () => {
      const mock: StockOptimizationResponse = {
        products: [], abcDistribution: {}, xyzDistribution: {}, abcxzDistribution: {},
        actionDistribution: {}, riskDistribution: {}, totalStockValue: 0,
        totalSafetyStockValue: 0, productsToOrder: 0, productsAtRisk: 0,
        productsOverstocked: 0, summary: 'test', calculatedAt: ''
      };
      service.optimize().subscribe(data => {
        expect(data.summary).toBe('test');
      });
      const req = httpMock.expectOne('/api/suppliers/1/optimization');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('configure', () => {
    it('should POST configuration params', () => {
      const mock: StockOptimizationResponse = {
        products: [], abcDistribution: {}, xyzDistribution: {}, abcxzDistribution: {},
        actionDistribution: {}, riskDistribution: {}, totalStockValue: 0,
        totalSafetyStockValue: 0, productsToOrder: 0, productsAtRisk: 0,
        productsOverstocked: 0, summary: 'configured', calculatedAt: ''
      };
      service.configure(7, 50, 0.25).subscribe(data => {
        expect(data.summary).toBe('configured');
      });
      const req = httpMock.expectOne(r => r.url === '/api/suppliers/1/optimization/configure');
      expect(req.request.method).toBe('POST');
      expect(req.request.params.get('leadTimeDays')).toBe('7');
      expect(req.request.params.get('orderingCost')).toBe('50');
      expect(req.request.params.get('holdingCostPercent')).toBe('0.25');
      req.flush(mock);
    });
  });

  describe('getSupplierId', () => {
    it('should return 0 when no user in session', () => {
      sessionStorage.clear();
      service.optimize().subscribe();
      const req = httpMock.expectOne(r => r.url === '/api/suppliers/0/optimization');
      req.flush({ summary: '' });
    });

    it('should return 0 when user has no organizationId', () => {
      sessionStorage.setItem('user', JSON.stringify({}));
      service.optimize().subscribe();
      const req = httpMock.expectOne(r => r.url === '/api/suppliers/0/optimization');
      req.flush({ summary: '' });
    });
  });
});
