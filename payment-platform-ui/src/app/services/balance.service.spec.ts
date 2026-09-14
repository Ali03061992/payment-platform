import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { BalanceService } from './balance.service';
import { BalanceSummary, BalanceEntry } from '../models/balance.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('BalanceService', () => {
  let service: BalanceService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
    imports: [],
    providers: [BalanceService, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
});
    service = TestBed.inject(BalanceService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('listBySupplier', () => {
    it('should GET balances by supplier', () => {
      const mock: BalanceSummary[] = [
        { supplierId: 1, shopId: 2, supplierName: 'Sup', shopName: 'Shop', currentBalance: 500, totalOrders: 10, totalPayments: 5, remainingDue: 250, lastTransaction: '' }
      ];
      service.listBySupplier(1).subscribe(data => {
        expect(data.length).toBe(1);
        expect(data[0].remainingDue).toBe(250);
      });
      const req = httpMock.expectOne('/api/balances/supplier/1');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('listByShop', () => {
    it('should GET balances by shop', () => {
      const mock: BalanceSummary[] = [
        { supplierId: 1, shopId: 2, supplierName: 'Sup', shopName: 'Shop', currentBalance: 500, totalOrders: 10, totalPayments: 5, remainingDue: 250, lastTransaction: '' }
      ];
      service.listByShop(2).subscribe(data => {
        expect(data.length).toBe(1);
      });
      const req = httpMock.expectOne('/api/balances/shop/2');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('getHistory', () => {
    it('should GET balance history', () => {
      const mock: BalanceEntry[] = [
        { id: 1, supplierId: 1, shopId: 2, type: 'ORDER', amount: 100, balanceAfter: 400, orderId: 1, paymentId: null, reference: null, reason: null, createdBy: 1, createdAt: '' }
      ];
      service.getHistory(1, 2).subscribe(data => {
        expect(data.length).toBe(1);
        expect(data[0].type).toBe('ORDER');
      });
      const req = httpMock.expectOne('/api/balances/supplier/1/shop/2');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('adjust', () => {
    it('should POST to adjust balance', () => {
      const mock: BalanceEntry = { id: 1, supplierId: 1, shopId: 2, type: 'ADJUSTMENT', amount: 50, balanceAfter: 450, orderId: null, paymentId: null, reference: null, reason: 'Correction', createdBy: 1, createdAt: '' };
      service.adjust({ supplierId: 1, shopId: 2, amount: 50, reason: 'Correction' }).subscribe(data => {
        expect(data.type).toBe('ADJUSTMENT');
      });
      const req = httpMock.expectOne('/api/balances/adjust');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ supplierId: 1, shopId: 2, amount: 50, reason: 'Correction' });
      req.flush(mock);
    });
  });
});
