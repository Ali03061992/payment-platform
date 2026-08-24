import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { PaymentService } from './payment.service';
import { Payment, PaymentStats } from '../models/payment.model';

describe('PaymentService', () => {
  let service: PaymentService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [PaymentService]
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
        { id: 1, reference: 'PAY-001', shopId: 1, supplierId: 2, amount: 100, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, version: 1, createdAt: '', updatedAt: '', events: [] }
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
      const mock: Payment = { id: 1, reference: 'PAY-001', shopId: 1, supplierId: 2, amount: 100, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, version: 1, createdAt: '', updatedAt: '', events: [] };
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
      const mock: Payment = { id: 1, reference: 'PAY-001', shopId: 1, supplierId: 2, amount: 100, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, version: 1, createdAt: '', updatedAt: '', events: [] };
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
      const mock: Payment = { id: 1, reference: 'PAY-001', shopId: 1, supplierId: 2, amount: 100, currency: 'TND', status: 'PENDING', rejectionReason: '', createdBy: 1, version: 1, createdAt: '', updatedAt: '', events: [] };
      service.create({ shopId: 1, supplierId: 2, amount: 100, currency: 'TND' }).subscribe(data => {
        expect(data.amount).toBe(100);
      });
      const req = httpMock.expectOne('/api/payments');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ shopId: 1, supplierId: 2, amount: 100, currency: 'TND' });
      req.flush(mock);
    });
  });

  describe('confirm', () => {
    it('should POST to confirm payment', () => {
      const mock: Payment = { id: 1, reference: 'PAY-001', shopId: 1, supplierId: 2, amount: 100, currency: 'TND', status: 'CONFIRMED', rejectionReason: '', createdBy: 1, version: 2, createdAt: '', updatedAt: '', events: [] };
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
      const mock: Payment = { id: 1, reference: 'PAY-001', shopId: 1, supplierId: 2, amount: 100, currency: 'TND', status: 'REJECTED', rejectionReason: 'Invalid', createdBy: 1, version: 2, createdAt: '', updatedAt: '', events: [] };
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
      const mock: Payment = { id: 1, reference: 'PAY-001', shopId: 1, supplierId: 2, amount: 100, currency: 'TND', status: 'CANCELLED', rejectionReason: '', createdBy: 1, version: 2, createdAt: '', updatedAt: '', events: [] };
      service.cancel(1).subscribe(data => {
        expect(data.status).toBe('CANCELLED');
      });
      const req = httpMock.expectOne('/api/payments/1/cancel');
      expect(req.request.method).toBe('POST');
      req.flush(mock);
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
});
