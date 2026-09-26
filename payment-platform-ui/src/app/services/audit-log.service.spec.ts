// @ts-nocheck
/**
 * Tests du service AuditLogService.
 * Perimetre : cas should call list with default params; should pass filter params (voir blocs describe/it).
 * Moyens : HttpTestingController, TestBed, client HTTP de test.
 */
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuditLogService } from './audit-log.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('AuditLogService', () => {
  let service: AuditLogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
    imports: [],
    providers: [
        AuditLogService,
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    service = TestBed.inject(AuditLogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('should call list with default params', () => {
    service.list().subscribe();
    const req = httpMock.expectOne(r => r.url === '/api/admin/audit-logs');
    expect(req.request.method).toBe('GET');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 50 });
  });

  it('should pass filter params', () => {
    service.list({ userId: 'u1', action: 'PAYMENT_CREATED', dateFrom: '2025-01-01', dateTo: '2025-12-31', page: 2, size: 25 }).subscribe();
    const req = httpMock.expectOne(r => r.url === '/api/admin/audit-logs');
    expect(req.request.params.get('userId')).toBe('u1');
    expect(req.request.params.get('action')).toBe('PAYMENT_CREATED');
    expect(req.request.params.get('dateFrom')).toBe('2025-01-01');
    expect(req.request.params.get('dateTo')).toBe('2025-12-31');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('25');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 2, size: 25 });
  });
});
