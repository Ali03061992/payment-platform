// @ts-nocheck
/**
 * Tests du cache HTTP stale-while-revalidate.
 * Perimetre : hit mémoire, invalidation, mutations, exclusions JSON.
 */
import { TestBed } from '@angular/core/testing';
import { HttpClient, HTTP_INTERCEPTORS, provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { DataCacheInterceptor } from './data-cache.interceptor';
import { DataStoreService } from './data-store.service';

describe('DataCacheInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let store: DataStoreService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        { provide: HTTP_INTERCEPTORS, useClass: DataCacheInterceptor, multi: true },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    store = TestBed.inject(DataStoreService);
  });

  afterEach(() => {
    store.reset();
    httpMock.verify();
  });

  it('should serve the cached GET without a second HTTP call', () => {
    let first: unknown[] = [];
    http.get<unknown[]>('/api/orders').subscribe((data) => first = data);
    httpMock.expectOne('/api/orders').flush([{ id: 'o1' }]);
    expect(first.length).toBe(1);

    let second: unknown[] = [];
    http.get<unknown[]>('/api/orders').subscribe((data) => second = data);
    httpMock.expectNone('/api/orders');
    expect(second.length).toBe(1);
  });

  it('should revalidate after invalidation', () => {
    http.get('/api/orders').subscribe();
    httpMock.expectOne('/api/orders').flush([{ id: 'o1' }]);

    store.invalidateAll();

    let refreshed: unknown[] = [];
    http.get<unknown[]>('/api/orders').subscribe((data) => refreshed = data);
    const req = httpMock.expectOne('/api/orders');
    req.flush([{ id: 'o2' }]);
    expect(refreshed).toEqual([{ id: 'o2' }]);
  });

  it('should invalidate the store after a successful mutation', () => {
    http.get('/api/orders').subscribe();
    httpMock.expectOne('/api/orders').flush([{ id: 'o1' }]);

    http.post('/api/orders', {}).subscribe();
    httpMock.expectOne('/api/orders').flush({ id: 'o2' });

    let refreshed: unknown[] = [];
    http.get<unknown[]>('/api/orders').subscribe((data) => refreshed = data);
    httpMock.expectOne('/api/orders').flush([{ id: 'o1' }, { id: 'o2' }]);
    expect(refreshed.length).toBe(2);
  });

  it('should not cache auth, notification or blob requests', () => {
    const auth: unknown[] = [];
    http.get<unknown>('/api/auth/me').subscribe((data) => auth.push(data));
    httpMock.expectOne('/api/auth/me').flush({ id: 'u1' });
    http.get<unknown>('/api/auth/me').subscribe((data) => auth.push(data));
    httpMock.expectOne('/api/auth/me').flush({ id: 'u2' });
    expect(auth).toEqual([{ id: 'u1' }, { id: 'u2' }]);

    const notifications: unknown[] = [];
    http.get<unknown[]>('/api/notifications').subscribe((data) => notifications.push(data));
    httpMock.expectOne('/api/notifications').flush([]);
    http.get<unknown[]>('/api/notifications').subscribe((data) => notifications.push(data));
    httpMock.expectOne('/api/notifications').flush([{ id: 'n1' }]);
    expect(notifications.length).toBe(2);

    const blobs: Blob[] = [];
    http.get('/api/orders/1/invoice', { responseType: 'blob' }).subscribe((data) => blobs.push(data));
    httpMock.expectOne('/api/orders/1/invoice').flush(new Blob(['pdf']));
    http.get('/api/orders/1/invoice', { responseType: 'blob' }).subscribe((data) => blobs.push(data));
    httpMock.expectOne('/api/orders/1/invoice').flush(new Blob(['pdf']));
    expect(blobs.length).toBe(2);
  });

  it('should bypass the cache when x-skip-cache is set', () => {
    const values: unknown[] = [];
    const headers = { 'x-skip-cache': 'true' };
    http.get('/api/orders', { headers }).subscribe((data) => values.push(data));
    httpMock.expectOne('/api/orders').flush([{ id: 'o1' }]);
    http.get('/api/orders', { headers }).subscribe((data) => values.push(data));
    httpMock.expectOne('/api/orders').flush([{ id: 'o2' }]);
    expect(values).toEqual([[{ id: 'o1' }], [{ id: 'o2' }]]);
  });
});
