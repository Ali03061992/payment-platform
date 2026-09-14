import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { OrderService } from './order.service';
import { Order } from '../models/order.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

const mockOrder: Order = {
  id: 1, reference: 'ORD-001', supplierId: 1, shopId: 2, createdBy: 3, createdByRole: 'SHOP_ADMIN',
  source: 'MANUAL', status: 'DRAFT', subtotal: 100, taxRate: 0.19, taxAmount: 19, total: 119,
  currency: 'TND', deliveryAgentId: null, receivedBy: null, receivedAt: null, deliveredAt: null,
  asapPayment: false, notes: '', version: 1, createdAt: '', updatedAt: '', items: [], events: []
};

describe('OrderService', () => {
  let service: OrderService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
    imports: [],
    providers: [OrderService, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
});
    service = TestBed.inject(OrderService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('create', () => {
    it('should POST to create order', () => {
      service.create({ supplierId: 1, shopId: 2, asapPayment: false, currency: 'TND', notes: '', items: [] }).subscribe(data => {
        expect(data.id).toBe(1);
      });
      const req = httpMock.expectOne('/api/orders');
      expect(req.request.method).toBe('POST');
      req.flush(mockOrder);
    });
  });

  describe('list', () => {
    it('should GET orders', () => {
      service.list().subscribe(data => {
        expect(data.length).toBe(1);
      });
      const req = httpMock.expectOne('/api/orders');
      expect(req.request.method).toBe('GET');
      req.flush([mockOrder]);
    });
  });

  describe('getById', () => {
    it('should GET order by id', () => {
      service.getById(1).subscribe(data => {
        expect(data.id).toBe(1);
      });
      const req = httpMock.expectOne('/api/orders/1');
      expect(req.request.method).toBe('GET');
      req.flush(mockOrder);
    });
  });

  describe('confirm', () => {
    it('should POST confirm', () => {
      service.confirm(1).subscribe(data => {
        expect(data.status).toBe('CONFIRMED');
      });
      const req = httpMock.expectOne('/api/orders/1/confirm');
      expect(req.request.method).toBe('POST');
      req.flush({ ...mockOrder, status: 'CONFIRMED' });
    });
  });

  describe('prepare', () => {
    it('should POST prepare', () => {
      service.prepare(1).subscribe();
      const req = httpMock.expectOne('/api/orders/1/prepare');
      expect(req.request.method).toBe('POST');
      req.flush(mockOrder);
    });
  });

  describe('readyForDelivery', () => {
    it('should POST ready', () => {
      service.readyForDelivery(1).subscribe();
      const req = httpMock.expectOne('/api/orders/1/ready');
      expect(req.request.method).toBe('POST');
      req.flush(mockOrder);
    });
  });

  describe('assignDelivery', () => {
    it('should POST assign-delivery with agentId', () => {
      service.assignDelivery(1, 5).subscribe();
      const req = httpMock.expectOne('/api/orders/1/assign-delivery');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ agentId: 5 });
      req.flush(mockOrder);
    });
  });

  describe('deliver', () => {
    it('should POST deliver with receivedBy', () => {
      service.deliver(1, 3).subscribe();
      const req = httpMock.expectOne('/api/orders/1/deliver');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ receivedBy: 3 });
      req.flush(mockOrder);
    });
  });

  describe('accept', () => {
    it('should POST accept', () => {
      service.accept(1).subscribe();
      const req = httpMock.expectOne('/api/orders/1/accept');
      expect(req.request.method).toBe('POST');
      req.flush(mockOrder);
    });
  });

  describe('acceptAsap', () => {
    it('should POST accept-asap', () => {
      service.acceptAsap(1).subscribe();
      const req = httpMock.expectOne('/api/orders/1/accept-asap');
      expect(req.request.method).toBe('POST');
      req.flush(mockOrder);
    });
  });

  describe('cancel', () => {
    it('should POST cancel', () => {
      service.cancel(1).subscribe();
      const req = httpMock.expectOne('/api/orders/1/cancel');
      expect(req.request.method).toBe('POST');
      req.flush(mockOrder);
    });
  });

  describe('reject', () => {
    it('should POST reject', () => {
      service.reject(1).subscribe();
      const req = httpMock.expectOne('/api/orders/1/reject');
      expect(req.request.method).toBe('POST');
      req.flush(mockOrder);
    });
  });

  describe('deliveryReject', () => {
    it('should POST delivery-reject with reason', () => {
      service.deliveryReject(1, 'Damaged').subscribe();
      const req = httpMock.expectOne('/api/orders/1/delivery-reject');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ reason: 'Damaged' });
      req.flush(mockOrder);
    });

    it('should POST delivery-reject without reason', () => {
      service.deliveryReject(1).subscribe();
      const req = httpMock.expectOne('/api/orders/1/delivery-reject');
      expect(req.request.body).toEqual({ reason: undefined });
      req.flush(mockOrder);
    });
  });

  describe('myDeliveries', () => {
    it('should GET my deliveries', () => {
      service.myDeliveries().subscribe(data => {
        expect(data.length).toBe(1);
      });
      const req = httpMock.expectOne('/api/orders/my-deliveries');
      expect(req.request.method).toBe('GET');
      req.flush([mockOrder]);
    });
  });
});
