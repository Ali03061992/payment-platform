// @ts-nocheck
/**
 * Tests du garde AuthGuard.
 * Perimetre : instanciation et blocs canActivate (voir blocs describe/it).
 * Moyens : TestBed, stubs jasmine.
 */
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthGuard } from './auth.guard';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        { provide: Router, useValue: routerSpy }
      ]
    });
    guard = TestBed.inject(AuthGuard);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    sessionStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  describe('canActivate', () => {
    it('should return false and redirect to /login when no token', () => {
      const result = guard.canActivate();
      expect(result).toBeFalse();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should return true when valid token exists', () => {
      const futureExp = Math.floor(Date.now() / 1000) + 3600;
      const payload = btoa(JSON.stringify({ exp: futureExp }));
      const token = `header.${payload}.signature`;
      sessionStorage.setItem('token', token);

      const result = guard.canActivate();
      expect(result).toBeTrue();
    });

    it('should return false when token is expired', () => {
      const pastExp = Math.floor(Date.now() / 1000) - 3600;
      const payload = btoa(JSON.stringify({ exp: pastExp }));
      const token = `header.${payload}.signature`;
      sessionStorage.setItem('token', token);

      const result = guard.canActivate();
      expect(result).toBeFalse();
      expect(sessionStorage.getItem('token')).toBeNull();
      expect(sessionStorage.getItem('user')).toBeNull();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should return false for malformed token', () => {
      sessionStorage.setItem('token', 'invalid-token');
      const result = guard.canActivate();
      expect(result).toBeFalse();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });
});
