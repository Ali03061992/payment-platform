// @ts-nocheck
import { TestBed } from '@angular/core/testing';
import { HttpRequest, HttpHandler, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { JwtInterceptor } from './jwt.interceptor';

describe('JwtInterceptor', () => {
  let interceptor: JwtInterceptor;
  let router: jasmine.SpyObj<Router>;
  let next: jasmine.SpyObj<HttpHandler>;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const nextSpy = jasmine.createSpyObj('HttpHandler', ['handle']);
    TestBed.configureTestingModule({
      providers: [
        JwtInterceptor,
        { provide: Router, useValue: routerSpy },
        { provide: HttpHandler, useValue: nextSpy }
      ]
    });
    interceptor = TestBed.inject(JwtInterceptor);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    next = TestBed.inject(HttpHandler) as jasmine.SpyObj<HttpHandler>;
    sessionStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(interceptor).toBeTruthy();
  });

  describe('intercept', () => {
    it('should not add Authorization header when no token', () => {
      const req = new HttpRequest('GET', '/api/test');
      next.handle.and.returnValue(of({} as any));
      interceptor.intercept(req, next);
      const handledReq = next.handle.calls.first().args[0];
      expect(handledReq.headers.has('Authorization')).toBeFalse();
    });

    it('should add Authorization header when token exists', () => {
      sessionStorage.setItem('token', 'test-jwt-token');
      const req = new HttpRequest('GET', '/api/test');
      next.handle.and.returnValue(of({} as any));
      interceptor.intercept(req, next);
      const handledReq = next.handle.calls.first().args[0];
      expect(handledReq.headers.get('Authorization')).toBe('Bearer test-jwt-token');
    });

    it('should return error as-is for status 0', () => {
      const error = new HttpErrorResponse({ status: 0, statusText: 'abort' });
      const req = new HttpRequest('GET', '/api/test');
      next.handle.and.returnValue(throwError(() => error));
      interceptor.intercept(req, next).subscribe({
        error: (e) => {
          expect(e.status).toBe(0);
        }
      });
    });

    it('should clear token and redirect for 401 error', () => {
      sessionStorage.setItem('token', 'expired-token');
      sessionStorage.setItem('user', '{"id":1}');
      const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
      const req = new HttpRequest('GET', '/api/test');
      next.handle.and.returnValue(throwError(() => error));
      interceptor.intercept(req, next).subscribe({
        error: (e) => {
          expect(e.status).toBe(401);
          expect(sessionStorage.getItem('token')).toBeNull();
          expect(sessionStorage.getItem('user')).toBeNull();
          expect(router.navigate).toHaveBeenCalledWith(['/login']);
        }
      });
    });

    it('should return error as-is for 403', () => {
      const error = new HttpErrorResponse({ status: 403, statusText: 'Forbidden' });
      const req = new HttpRequest('GET', '/api/test');
      next.handle.and.returnValue(throwError(() => error));
      interceptor.intercept(req, next).subscribe({
        error: (e) => {
          expect(e.status).toBe(403);
          expect(router.navigate).not.toHaveBeenCalled();
        }
      });
    });

    it('should not redirect for 500 error', () => {
      const error = new HttpErrorResponse({ status: 500, statusText: 'Server Error' });
      const req = new HttpRequest('GET', '/api/test');
      next.handle.and.returnValue(throwError(() => error));
      interceptor.intercept(req, next).subscribe({
        error: (e) => {
          expect(e.status).toBe(500);
          expect(router.navigate).not.toHaveBeenCalled();
        }
      });
    });
  });
});
