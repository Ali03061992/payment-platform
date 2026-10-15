// @ts-nocheck
/**
 * Tests du service QuoteService (demandes de devis découplées).
 */
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { QuoteService } from './quote.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { translateServiceProvider } from '../testing/translate-stubs';

describe('QuoteService', () => {
  let service: QuoteService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [QuoteService, translateServiceProvider(), provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(QuoteService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should POST demande without discount', () => {
    service.createRequest('sup-1', 'Urgent', [{ productId: 'p1', quantity: 2 }]).subscribe(r => {
      expect(r.reference).toBe('DEV-001');
    });
    const req = httpMock.expectOne('/api/quotes/requests');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ supplierId: 'sup-1', notes: 'Urgent', items: [{ productId: 'p1', quantity: 2 }] });
    req.flush({ reference: 'DEV-001' } as any);
  });

  it('should GET my requests', () => {
    service.myRequests().subscribe(list => expect(list.length).toBe(1));
    const req = httpMock.expectOne('/api/quotes/requests');
    expect(req.request.method).toBe('GET');
    req.flush([{ id: 'd1' }] as any);
  });

  it('should GET request detail', () => {
    service.getRequest('d1').subscribe(r => expect(r.id).toBe('d1'));
    httpMock.expectOne('/api/quotes/requests/d1').flush({ id: 'd1' } as any);
  });

  it('should POST quote with line discounts only', () => {
    service.sendQuote('d1', [{ productId: 'p1', quantity: 2, discount: 10 }]).subscribe(q => {
      expect(q.version).toBe(1);
    });
    const req = httpMock.expectOne('/api/quotes/requests/d1/quotes');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ items: [{ productId: 'p1', quantity: 2, discount: 10 }] });
    req.flush({ version: 1 } as any);
  });

  it('should POST accept and PATCH cancel', () => {
    service.acceptQuote('q1').subscribe(o => expect(o.id).toBe('ord-1'));
    httpMock.expectOne('/api/quotes/quotes/q1/accept').flush({ id: 'ord-1' } as any);
    service.cancelRequest('d1').subscribe(r => expect(r.status).toBe('CANCELLED'));
    const cancel = httpMock.expectOne('/api/quotes/requests/d1/cancel');
    expect(cancel.request.method).toBe('PATCH');
    cancel.flush({ status: 'CANCELLED' } as any);
  });

  it('should download quote PDF as blob', () => {
    const blob = new Blob(['%PDF-1.4'], { type: 'application/pdf' });
    spyOn(window.URL, 'createObjectURL').and.returnValue('blob:test');
    spyOn(window.URL, 'revokeObjectURL');
    spyOn(HTMLAnchorElement.prototype, 'click');
    service.downloadQuotePdf('d1', 2, 'DEV-001');
    const req = httpMock.expectOne('/api/quotes/requests/d1/quotes/2/pdf');
    expect(req.request.method).toBe('GET');
    expect(req.request.responseType).toBe('blob');
    req.flush(blob, {
      headers: { 'Content-Disposition': 'attachment; filename="devis-DEV-001-v2.pdf"' }
    });
  });

  it('should parse lines safely', () => {
    expect(service.parseLines('[{"qty":2}]')).toEqual([{ qty: 2 }]);
    expect(service.parseLines('nope')).toEqual([]);
    expect(service.parseLines('')).toEqual([]);
  });
});
