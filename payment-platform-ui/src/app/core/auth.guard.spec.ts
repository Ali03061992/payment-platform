// @ts-nocheck
/**
 * Tests du garde AuthGuard.
 * Perimetre : instanciation et blocs canActivate (session LoginService + returnUrl).
 * Moyens : TestBed, stubs jasmine.
 */
import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { AuthGuard } from './auth.guard';
import { LoginService } from '../services/login.service';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let router: jasmine.SpyObj<Router>;
  let login: jasmine.SpyObj<LoginService>;
  const state = { url: '/dashboard/shop/orders' } as any;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const loginSpy = jasmine.createSpyObj('LoginService', ['isLoggedIn', 'purgeSession']);
    TestBed.configureTestingModule({
      providers: [
        AuthGuard,
        { provide: Router, useValue: routerSpy },
        { provide: LoginService, useValue: loginSpy }
      ]
    });
    guard = TestBed.inject(AuthGuard);
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    login = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
  });

  it('should be created', () => {
    expect(guard).toBeTruthy();
  });

  describe('canActivate', () => {
    it('should return false and redirect to /login with returnUrl when no session', () => {
      login.isLoggedIn.and.returnValue(false);
      const result = guard.canActivate({} as any, state);
      expect(result).toBeFalse();
      expect(login.purgeSession).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/login'], { queryParams: { returnUrl: '/dashboard/shop/orders' } });
    });

    it('should return true when in-memory token is valid', () => {
      login.isLoggedIn.and.returnValue(true);
      const result = guard.canActivate({} as any, state);
      expect(result).toBeTrue();
      expect(router.navigate).not.toHaveBeenCalled();
    });
  });
});
