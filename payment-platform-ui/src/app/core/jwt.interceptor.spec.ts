// @ts-nocheck
/**
 * Tests du intercepteur JwtInterceptor.
 * Perimetre : instanciation et blocs intercept (token mémoire + refresh cookie single-flight).
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
    const loginSpy = jasmine.createSpyObj('LoginService', ['getToken', 'refresh', 'purgeSession']);
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
    loginService.getToken.and.returnValue(null);
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
      loginService.getToken.and.returnValue('test-jwt-token');
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

    it('should purge session and redirect for 401 error on login call', () => {
      loginService.getToken.and.returnValue('expired-token');
      const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
      const req = new HttpRequest('GET', '/api/auth/login');
      next.handle.and.returnValue(throwError(() => error));
      interceptor.intercept(req, next).subscribe({
        error: (e) => {
          expect(e.status).toBe(401);
          expect(loginService.purgeSession).toHaveBeenCalled();
          expect(router.navigate).toHaveBeenCalledWith(['/login']);
        }
      });
    });

    it('should logout when silent refresh fails (cookie absent/expiré)', () => {
      loginService.getToken.and.returnValue('valid-token');
      const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
      const req = new HttpRequest('GET', '/api/test');
      next.handle.and.returnValue(throwError(() => error));
      loginService.refresh.and.returnValue(throwError(() => new HttpErrorResponse({ status: 401 })));
      interceptor.intercept(req, next).subscribe({
        error: (e) => {
          expect(e.status).toBe(401);
          expect(loginService.purgeSession).toHaveBeenCalled();
          expect(router.navigate).toHaveBeenCalledWith(['/login']);
        }
      });
    });

    it('should silently refresh and retry the request on 401', () => {
      loginService.getToken.and.returnValue('expired-token');
      const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
      const req = new HttpRequest('GET', '/api/test');
      next.handle.and.returnValues(throwError(() => error), of({ retried: true } as any));
      loginService.refresh.and.returnValue(of('fresh-token'));
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

    it('should not attempt refresh for auth endpoints', () => {
      loginService.getToken.and.returnValue('t');
      const error = new HttpErrorResponse({ status: 401, statusText: 'Unauthorized' });
      const req = new HttpRequest('POST', '/api/auth/refresh', {});
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
