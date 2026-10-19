// @ts-nocheck
/**
 * Tests du garde RoleGuard.
 * Perimetre : instanciation et blocs canActivate (session LoginService + rôles).
 * Moyens : TestBed, stubs jasmine.
 */
import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot } from '@angular/router';
import { RoleGuard } from './role.guard';
import { LoginService } from '../services/login.service';

describe('RoleGuard', () => {
  let guard: RoleGuard;
  let router: jasmine.SpyObj<Router>;
  let login: jasmine.SpyObj<LoginService>;
  const state = { url: '/dashboard/admin/users' } as any;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const loginSpy = jasmine.createSpyObj('LoginService', ['isLoggedIn', 'getCurrentUser', 'purgeSession']);
    TestBed.configureTestingModule({
      providers: [
        RoleGuard,
        { provide: Router, useValue: routerSpy },
        { provide: LoginService, useValue: loginSpy }
      ]
    });
    guard = TestBed.inject(RoleGuard);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    login = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  function createRoute(data: any): ActivatedRouteSnapshot {
    return { data } as ActivatedRouteSnapshot;
  }

  describe('canActivate', () => {
    it('should return false and redirect to /login with returnUrl when no session', () => {
      login.isLoggedIn.and.returnValue(false);
      const route = createRoute({ roles: ['SYSTEM_ADMIN'] });
      const result = guard.canActivate(route, state);
      expect(result).toBeFalse();
      expect(login.purgeSession).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: state.url } });
    });

    it('should return false when no user persisted', () => {
      login.isLoggedIn.and.returnValue(true);
      login.getCurrentUser.and.returnValue(null);
      const route = createRoute({ roles: ['SYSTEM_ADMIN'] });
      const result = guard.canActivate(route, state);
      expect(result).toBeFalse();
      expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: state.url } });
    });

    it('should return true when roles array is empty', () => {
      login.isLoggedIn.and.returnValue(true);
      login.getCurrentUser.and.returnValue({ roles: ['SHOP_AGENT'] } as any);
      const route = createRoute({ roles: [] });
      const result = guard.canActivate(route, state);
      expect(result).toBeTrue();
    });

    it('should return true when user has required role', () => {
      login.isLoggedIn.and.returnValue(true);
      login.getCurrentUser.and.returnValue({ roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN'] } as any);
      const route = createRoute({ roles: ['SYSTEM_ADMIN'] });
      const result = guard.canActivate(route, state);
      expect(result).toBeTrue();
    });

    it('should return false when user does not have required role', () => {
      login.isLoggedIn.and.returnValue(true);
      login.getCurrentUser.and.returnValue({ roles: ['SHOP_AGENT'] } as any);
      const route = createRoute({ roles: ['SYSTEM_ADMIN'] });
      const result = guard.canActivate(route, state);
      expect(result).toBeFalse();
      expect(router.navigate).toHaveBeenCalledWith(['/403']);
    });

    it('should return true when user has one of multiple required roles', () => {
      login.isLoggedIn.and.returnValue(true);
      login.getCurrentUser.and.returnValue({ roles: ['SUPPLIER_ADMIN'] } as any);
      const route = createRoute({ roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN'] });
      const result = guard.canActivate(route, state);
      expect(result).toBeTrue();
    });

    it('should handle missing roles in route data', () => {
      login.isLoggedIn.and.returnValue(true);
      login.getCurrentUser.and.returnValue({ roles: ['SHOP_AGENT'] } as any);
      const route = createRoute({});
      const result = guard.canActivate(route, state);
      expect(result).toBeTrue();
    });
  });
});
