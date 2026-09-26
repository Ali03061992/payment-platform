// @ts-nocheck
/**
 * Tests du service PaymentService.
 * Perimetre : instanciation et blocs list, getById, getByReference, create, confirm (voir blocs describe/it).
 * Moyens : HttpTestingController, TestBed, client HTTP de test.
 */
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { PaymentService } from './payment.service';
import { Payment, PaymentStats } from '../models/payment.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
    imports: [],
    providers: [PaymentService, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
});
    service = TestBed.inject(PaymentService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('list', () => {
    it('should GET payments', () => {
      const mock: Payment[] = [
        { id: 1, reference: 'PAY-001', shopId: 1, shopName: 'Shop', supplierId: 2, supplierName: 'Sup', amount: 100, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, createdByName: '', confirmedByName: '', rejectedByName: '', cancelledByName: '', version: 1, createdAt: '', updatedAt: '', events: [] }
      ];
      service.list().subscribe(data => {
        expect(data.length).toBe(1);
        expect(data[0].reference).toBe('PAY-001');
      });
      const req = httpMock.expectOne('/api/payments');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('getById', () => {
    it('should GET payment by id', () => {
      const mock: Payment = { id: 1, reference: 'PAY-001', shopId: 1, shopName: 'Shop', supplierId: 2, supplierName: 'Sup', amount: 100, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, createdByName: '', confirmedByName: '', rejectedByName: '', cancelledByName: '', version: 1, createdAt: '', updatedAt: '', events: [] };
      service.getById(1).subscribe(data => {
        expect(data.id).toBe(1);
      });
      const req = httpMock.expectOne('/api/payments/1');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('getByReference', () => {
    it('should GET payment by reference', () => {
      const mock: Payment = { id: 1, reference: 'PAY-001', shopId: 1, shopName: 'Shop', supplierId: 2, supplierName: 'Sup', amount: 100, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, createdByName: '', confirmedByName: '', rejectedByName: '', cancelledByName: '', version: 1, createdAt: '', updatedAt: '', events: [] };
      service.getByReference('PAY-001').subscribe(data => {
        expect(data.reference).toBe('PAY-001');
      });
      const req = httpMock.expectOne('/api/payments/reference/PAY-001');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('create', () => {
    it('should POST to create payment', () => {
      const mock: Payment = { id: 1, reference: 'PAY-001', shopId: 1, shopName: 'Shop', supplierId: 2, supplierName: 'Sup', amount: 100, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, createdByName: '', confirmedByName: '', rejectedByName: '', cancelledByName: '', version: 1, createdAt: '', updatedAt: '', events: [] };
      service.create({ shopId: 1, supplierId: 2, amount: 100, currency: 'TND' }).subscribe(data => {
        expect(data.amount).toBe(100);
      });
      const req = httpMock.expectOne('/api/payments');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ shopId: 1, supplierId: 2, amount: 100, currency: 'TND' });
      req.flush(mock);
    });

    it('should send an Idempotency-Key header (generated when absent)', () => {
      const mock: Payment = { id: 1, reference: 'PAY-001', shopId: 1, shopName: 'Shop', supplierId: 2, supplierName: 'Sup', amount: 100, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, createdByName: '', confirmedByName: '', rejectedByName: '', cancelledByName: '', version: 1, createdAt: '', updatedAt: '', events: [] };
      service.create({ shopId: 1, supplierId: 2, amount: 100, currency: 'TND' }).subscribe();
      const req = httpMock.expectOne('/api/payments');
      expect(req.request.headers.get('Idempotency-Key')).toBeTruthy();
      req.flush(mock);
    });

    it('should reuse the provided Idempotency-Key on retry', () => {
      const mock: Payment = { id: 1, reference: 'PAY-001', shopId: 1, shopName: 'Shop', supplierId: 2, supplierName: 'Sup', amount: 100, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, createdByName: '', confirmedByName: '', rejectedByName: '', cancelledByName: '', version: 1, createdAt: '', updatedAt: '', events: [] };
      service.create({ shopId: 1, supplierId: 2, amount: 100, currency: 'TND' }, 'key-123').subscribe();
      const req = httpMock.expectOne('/api/payments');
      expect(req.request.headers.get('Idempotency-Key')).toBe('key-123');
      req.flush(mock);
    });
  });

  describe('confirm', () => {
    it('should POST to confirm payment', () => {
      const mock: Payment = { id: 1, reference: 'PAY-001', shopId: 1, shopName: 'Shop', supplierId: 2, supplierName: 'Sup', amount: 100, currency: 'TND', status: 'CONFIRMED', rejectionReason: '', createdBy: 1, createdByName: '', confirmedByName: '', rejectedByName: '', cancelledByName: '', version: 2, createdAt: '', updatedAt: '', events: [] };
      service.confirm(1).subscribe(data => {
        expect(data.status).toBe('CONFIRMED');
      });
      const req = httpMock.expectOne('/api/payments/1/confirm');
      expect(req.request.method).toBe('POST');
      req.flush(mock);
    });
  });

  describe('reject', () => {
    it('should POST to reject payment with reason', () => {
      const mock: Payment = { id: 1, reference: 'PAY-001', shopId: 1, shopName: 'Shop', supplierId: 2, supplierName: 'Sup', amount: 100, currency: 'TND', status: 'REJECTED', rejectionReason: 'Invalid', createdBy: 1, createdByName: '', confirmedByName: '', rejectedByName: '', cancelledByName: '', version: 2, createdAt: '', updatedAt: '', events: [] };
      service.reject(1, { rejectionReason: 'Invalid' }).subscribe(data => {
        expect(data.status).toBe('REJECTED');
        expect(data.rejectionReason).toBe('Invalid');
      });
      const req = httpMock.expectOne('/api/payments/1/reject');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ rejectionReason: 'Invalid' });
      req.flush(mock);
    });
  });

  describe('cancel', () => {
    it('should POST to cancel payment', () => {
      const mock: Payment = { id: 1, reference: 'PAY-001', shopId: 1, shopName: 'Shop', supplierId: 2, supplierName: 'Sup', amount: 100, currency: 'TND', status: 'CANCELLED', rejectionReason: '', createdBy: 1, createdByName: '', confirmedByName: '', rejectedByName: '', cancelledByName: '', version: 2, createdAt: '', updatedAt: '', events: [] };
      service.cancel(1).subscribe(data => {
        expect(data.status).toBe('CANCELLED');
      });
      const req = httpMock.expectOne('/api/payments/1/cancel');
      expect(req.request.method).toBe('POST');
      req.flush(mock);
    });
  });

  describe('downloadInvoice', () => {
    it('should GET invoice as blob and save it', () => {
      const blob = new Blob(['%PDF-1.4'], { type: 'application/pdf' });
      const createSpy = spyOn(window.URL, 'createObjectURL').and.returnValue('blob:test');
      const revokeSpy = spyOn(window.URL, 'revokeObjectURL');
      const clickSpy = spyOn(HTMLAnchorElement.prototype, 'click');

      service.downloadInvoice('abc-123');

      const req = httpMock.expectOne('/api/payments/abc-123/invoice');
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(blob, {
        headers: { 'Content-Disposition': 'attachment; filename="facture-REF-1.pdf"' }
      });

      expect(createSpy).toHaveBeenCalled();
      expect(clickSpy).toHaveBeenCalled();
      expect(revokeSpy).toHaveBeenCalled();
    });
  });

  describe('exportCsv', () => {
    it('should GET CSV as blob and save it', () => {
      const blob = new Blob(['ref,status'], { type: 'text/csv' });
      const createSpy = spyOn(window.URL, 'createObjectURL').and.returnValue('blob:test');
      const revokeSpy = spyOn(window.URL, 'revokeObjectURL');
      const clickSpy = spyOn(HTMLAnchorElement.prototype, 'click');

      service.exportCsv({ status: 'PENDING' });

      const req = httpMock.expectOne('/api/payments/export/csv?status=PENDING');
      expect(req.request.method).toBe('GET');
      expect(req.request.responseType).toBe('blob');
      req.flush(blob, {
        headers: { 'Content-Disposition': 'attachment; filename="paiements.csv"' }
      });

      expect(createSpy).toHaveBeenCalled();
      expect(clickSpy).toHaveBeenCalled();
      expect(revokeSpy).toHaveBeenCalled();
    });
  });

  describe('getStats', () => {
    it('should GET stats', () => {
      const mock: PaymentStats = { total: 50, pending: 10, confirmed: 25, rejected: 10, cancelled: 5 };
      service.getStats().subscribe(data => {
        expect(data.total).toBe(50);
        expect(data.pending).toBe(10);
      });
      const req = httpMock.expectOne('/api/payments/stats');
      expect(req.request.method).toBe('GET');
      req.flush(mock);
    });
  });

  describe('getAgentSummary', () => {
    it('should GET agent summary with params', () => {
      const mock = [
        { userId: 1, username: 'agent1', paymentCount: 5, totalAmount: 500, confirmedTotal: 300, currency: 'TND', payments: [] }
      ];
      service.getAgentSummary(1, '2024-01-01', '2024-01-31').subscribe(data => {
        expect(data.length).toBe(1);
        expect(data[0].userId).toBe(1);
      });
      const req = httpMock.expectOne(r => r.url === '/api/payments/agent-summary');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('supplierId')).toBe('1');
      expect(req.request.params.get('from')).toBe('2024-01-01');
      expect(req.request.params.get('to')).toBe('2024-01-31');
      req.flush(mock);
    });
  });

  describe('list branches', () => {
    it('should handle response with items property', () => {
      const mock = [{ id: 1, reference: 'PAY-002', shopId: 1, shopName: 'Shop', supplierId: 2, supplierName: 'Sup', amount: 200, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, createdByName: '', confirmedByName: '', rejectedByName: '', cancelledByName: '', version: 1, createdAt: '', updatedAt: '', events: [] }];
      service.list().subscribe(data => {
        expect(data.length).toBe(1);
        expect(data[0].reference).toBe('PAY-002');
      });
      const req = httpMock.expectOne('/api/payments');
      req.flush({ items: mock });
    });

    it('should handle response with content property', () => {
      const mock = [{ id: 2, reference: 'PAY-003', shopId: 1, shopName: 'Shop', supplierId: 2, supplierName: 'Sup', amount: 300, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, createdByName: '', confirmedByName: '', rejectedByName: '', cancelledByName: '', version: 1, createdAt: '', updatedAt: '', events: [] }];
      service.list().subscribe(data => {
        expect(data.length).toBe(1);
      });
      const req = httpMock.expectOne('/api/payments');
      req.flush({ content: mock });
    });

    it('should handle response with data property', () => {
      const mock = [{ id: 3, reference: 'PAY-004', shopId: 1, shopName: 'Shop', supplierId: 2, supplierName: 'Sup', amount: 400, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, createdByName: '', confirmedByName: '', rejectedByName: '', cancelledByName: '', version: 1, createdAt: '', updatedAt: '', events: [] }];
      service.list().subscribe(data => {
        expect(data.length).toBe(1);
      });
      const req = httpMock.expectOne('/api/payments');
      req.flush({ data: mock });
    });

    it('should handle empty object response', () => {
      service.list().subscribe(data => {
        expect(data).toEqual([]);
      });
      const req = httpMock.expectOne('/api/payments');
      req.flush({});
    });

    it('should handle null response', () => {
      service.list().subscribe(data => {
        expect(data).toEqual([]);
      });
      const req = httpMock.expectOne('/api/payments');
      req.flush(null);
    });

    it('should handle error propagation', () => {
      let errorCaught = false;
      service.list().subscribe({
        next: () => fail('should not succeed'),
        error: () => errorCaught = true
      });
      const req = httpMock.expectOne('/api/payments');
      req.flush('error', { status: 500, statusText: 'Server Error' });
      expect(errorCaught).toBeTrue();
    });

    it('should handle empty array directly', () => {
      service.list().subscribe(data => {
        expect(data).toEqual([]);
      });
      const req = httpMock.expectOne('/api/payments');
      req.flush([]);
    });
  });
});
