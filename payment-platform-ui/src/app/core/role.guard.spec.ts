// @ts-nocheck
/**
 * Tests du garde RoleGuard.
 * Perimetre : instanciation et blocs canActivate (voir blocs describe/it).
 * Moyens : TestBed, stubs jasmine.
 */
import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot } from '@angular/router';
import { RoleGuard } from './role.guard';

describe('RoleGuard', () => {
  let guard: RoleGuard;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    TestBed.configureTestingModule({
      providers: [
        RoleGuard,
        { provide: Router, useValue: routerSpy }
      ]
    });
    guard = TestBed.inject(RoleGuard);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    sessionStorage.clear();
  });

  afterEach(() => {
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  function createRoute(data: any): ActivatedRouteSnapshot {
    return { data } as ActivatedRouteSnapshot;
  }

  describe('canActivate', () => {
    it('should return false and redirect to /login when no token', () => {
      const route = createRoute({ roles: ['SYSTEM_ADMIN'] });
      const result = guard.canActivate(route);
      expect(result).toBeFalse();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should return false when no user in session', () => {
      sessionStorage.setItem('token', 'test-token');
      const route = createRoute({ roles: ['SYSTEM_ADMIN'] });
      const result = guard.canActivate(route);
      expect(result).toBeFalse();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });

    it('should return true when roles array is empty', () => {
      sessionStorage.setItem('token', 'test-token');
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SHOP_AGENT'] }));
      const route = createRoute({ roles: [] });
      const result = guard.canActivate(route);
      expect(result).toBeTrue();
    });

    it('should return true when user has required role', () => {
      sessionStorage.setItem('token', 'test-token');
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN'] }));
      const route = createRoute({ roles: ['SYSTEM_ADMIN'] });
      const result = guard.canActivate(route);
      expect(result).toBeTrue();
    });

    it('should return false when user does not have required role', () => {
      sessionStorage.setItem('token', 'test-token');
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SHOP_AGENT'] }));
      const route = createRoute({ roles: ['SYSTEM_ADMIN'] });
      const result = guard.canActivate(route);
      expect(result).toBeFalse();
      expect(router.navigate).toHaveBeenCalledWith(['/403']);
    });

    it('should return true when user has one of multiple required roles', () => {
      sessionStorage.setItem('token', 'test-token');
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SUPPLIER_ADMIN'] }));
      const route = createRoute({ roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN'] });
      const result = guard.canActivate(route);
      expect(result).toBeTrue();
    });

    it('should handle missing roles in route data', () => {
      sessionStorage.setItem('token', 'test-token');
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SHOP_AGENT'] }));
      const route = createRoute({});
      const result = guard.canActivate(route);
      expect(result).toBeTrue();
    });
  });
});
