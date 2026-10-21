// @ts-nocheck
/**
 * Tests du garde AuthGuard.
 * Perimetre : instanciation et blocs canActivate (session LoginService + returnUrl).
 * Moyens : TestBed, stubs jasmine.
 */
import { TestBed } from '@angular/core/testing';
import { Router, UrlTree } from '@angular/router';
import { Observable, firstValueFrom, of } from 'rxjs';
import { AuthGuard } from './auth.guard';
import { LoginService } from '../services/login.service';

describe('AuthGuard', () => {
  let guard: AuthGuard;
  let router: jasmine.SpyObj<Router>;
  let login: jasmine.SpyObj<LoginService>;
  let loginTree: UrlTree;
  const state = { url: '/dashboard/shop/orders' } as any;

  const activate = (): Promise<boolean | UrlTree> =>
    firstValueFrom(guard.canActivate({} as any, state) as Observable<boolean | UrlTree>);

  beforeEach(() => {
    loginTree = {} as UrlTree;
    const routerSpy = jasmine.createSpyObj('Router', ['navigate', 'createUrlTree']);
    routerSpy.createUrlTree.and.returnValue(loginTree);
    const loginSpy = jasmine.createSpyObj('LoginService', ['isLoggedIn', 'restoreSession', 'purgeSession']);
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
    it('should silently refresh then redirect to /login with returnUrl when no session', async () => {
      login.isLoggedIn.and.returnValue(false);
      login.restoreSession.and.returnValue(of(false));

      const result = await activate();

      expect(result).toBe(loginTree);
      expect(login.restoreSession).toHaveBeenCalled();
      expect(login.purgeSession).not.toHaveBeenCalled();
      expect(router.createUrlTree).toHaveBeenCalledWith(
        ['/login'], { queryParams: { returnUrl: '/dashboard/shop/orders' } });
    });

    it('should allow navigation after a successful silent refresh', async () => {
      login.isLoggedIn.and.returnValue(false);
      login.restoreSession.and.returnValue(of(true));

      await expectAsync(activate()).toBeResolvedTo(true);
    });

    it('should return true without refresh when in-memory token is valid', async () => {
      login.isLoggedIn.and.returnValue(true);

      await expectAsync(activate()).toBeResolvedTo(true);
      expect(login.restoreSession).not.toHaveBeenCalled();
    });
  });
});
