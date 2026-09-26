// @ts-nocheck
/**
 * Tests du intercepteur JwtInterceptor.
 * Perimetre : instanciation et blocs intercept (voir blocs describe/it).
 * Moyens : TestBed, stubs jasmine, observables RxJS mockes.
 */
import { TestBed } from '@angular/core/testing';
import { HttpRequest, HttpHandler, HttpErrorResponse } from '@angular/common/http';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { JwtInterceptor } from './jwt.interceptor';
import { LoginService } from '../services/login.service';

describe('JwtInterceptor', () => {
  let interceptor: JwtInterceptor;
  let router: jasmine.SpyObj<Router>;
  let next: jasmine.SpyObj<HttpHandler>;
  let loginService: jasmine.SpyObj<LoginService>;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const nextSpy = jasmine.createSpyObj('HttpHandler', ['handle']);
    const loginSpy = jasmine.createSpyObj('LoginService', ['refresh']);
    TestBed.configureTestingModule({
      providers: [
        JwtInterceptor,
        { provide: Router, useValue: routerSpy },
        { provide: HttpHandler, useValue: nextSpy },
        { provide: LoginService, useValue: loginSpy }
      ]
    });
    interceptor = TestBed.inject(JwtInterceptor);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    next = TestBed.inject(HttpHandler) as jasmine.SpyObj<HttpHandler>;
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
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

    it('should clear token and redirect for 401 error on login call', () => {
      sessionStorage.setItem('token', 'expired-token');
      sessionStorage.setItem('user', '{"id":1}');
      const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
      const req = new HttpRequest('GET', '/api/auth/login');
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

    it('should clear session and redirect for 401 without refresh token', () => {
      sessionStorage.setItem('token', 'valid-token');
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
          expect(loginService.refresh).not.toHaveBeenCalled();
        }
      });
    });

    it('should silently refresh and retry the request on 401', () => {
      sessionStorage.setItem('token', 'expired-token');
      sessionStorage.setItem('refreshToken', 'stored-refresh');
      const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
      const req = new HttpRequest('GET', '/api/test');
      next.handle.and.returnValues(throwError(() => error), of({ retried: true } as any));
      loginService.refresh.and.callFake(() => {
        sessionStorage.setItem('token', 'fresh-token');
        return of({ accessToken: 'fresh-token' } as any);
      });
      interceptor.intercept(req, next).subscribe({
        next: (event: any) => {
          expect(event.retried).toBeTrue();
        }
      });
      expect(loginService.refresh).toHaveBeenCalledTimes(1);
      const retriedReq = next.handle.calls.argsFor(1)[0];
      expect(retriedReq.headers.get('Authorization')).toBe('Bearer fresh-token');
      expect(router.navigate).not.toHaveBeenCalled();
    });

    it('should logout when silent refresh fails', () => {
      sessionStorage.setItem('token', 'expired-token');
      sessionStorage.setItem('refreshToken', 'revoked-refresh');
      sessionStorage.setItem('user', '{"id":1}');
      const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
      const req = new HttpRequest('GET', '/api/test');
      next.handle.and.returnValue(throwError(() => error));
      loginService.refresh.and.returnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
      interceptor.intercept(req, next).subscribe({
        error: (e) => {
          expect(e.status).toBe(401);
          expect(sessionStorage.getItem('token')).toBeNull();
          expect(router.navigate).toHaveBeenCalledWith(['/login']);
        }
      });
    });

    it('should not attempt refresh for auth endpoints', () => {
      sessionStorage.setItem('token', 't');
      sessionStorage.setItem('refreshToken', 'r');
      const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
      const req = new HttpRequest('POST', '/api/auth/refresh', { refreshToken: 'r' });
      next.handle.and.returnValue(throwError(() => error));
      interceptor.intercept(req, next).subscribe({
        error: (e) => {
          expect(e.status).toBe(401);
          expect(loginService.refresh).not.toHaveBeenCalled();
          expect(router.navigate).not.toHaveBeenCalled();
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
