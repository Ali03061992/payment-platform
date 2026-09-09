import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PaymentSearchService, SearchPaymentsRequest } from './payment-search.service';

describe('PaymentSearchService', () => {
  let service: PaymentSearchService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PaymentSearchService]
    });
    service = TestBed.inject(PaymentSearchService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('search', () => {
    it('should POST search request', () => {
      const request: SearchPaymentsRequest = { page: 0, size: 20, status: 'PENDING' };
      const mockResponse = {
        payments: [{ id: 1, reference: 'PAY-001', shopId: 1, shopName: 'Shop', supplierId: 2, supplierName: 'Sup', createdBy: 1, createdByName: 'Admin', amount: 100, currency: 'TND', status: 'PENDING', rejectionReason: '', createdAt: '', updatedAt: '' }],
        total: 1,
        page: 0,
        size: 20
      };
      service.search(request).subscribe(res => {
        expect(res.payments.length).toBe(1);
        expect(res.total).toBe(1);
      });
      const req = httpMock.expectOne('/api/payments/search');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual(request);
      req.flush(mockResponse);
    });

    it('should POST with all filters', () => {
      const request: SearchPaymentsRequest = {
        shopId: 1, supplierId: 2, createdBy: 3, status: 'CONFIRMED',
        from: '2024-01-01', to: '2024-12-31', page: 1, size: 10
      };
      service.search(request).subscribe();
      const req = httpMock.expectOne('/api/payments/search');
      expect(req.request.body).toEqual(request);
      req.flush({ payments: [], total: 0, page: 1, size: 10 });
    });
  });
});
