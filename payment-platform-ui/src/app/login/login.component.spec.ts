// @ts-nocheck
/**
 * Tests du composant LoginComponent.
 * Perimetre : instanciation et blocs onSubmit (login adopte la session, returnUrl, purge).
 * Moyens : TestBed + fixture, stubs jasmine, client HTTP de test, observables RxJS mockes.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { LoginService } from '../services/login.service';
import { NotificationService } from '../services/notification.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { TranslateStubPipe, translateServiceProvider } from '../testing/translate-stubs';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let loginService: jasmine.SpyObj<LoginService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const loginSpy = jasmine.createSpyObj('LoginService',
      ['login', 'getMe', 'adoptSession', 'setCurrentUser', 'purgeSession', 'isLoggedIn']);
    const notifSpy = jasmine.createSpyObj('NotificationService', ['requestPermission']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate', 'navigateByUrl']);
    const routeStub = { snapshot: { queryParamMap: { get: () => null } } };

    TestBed.configureTestingModule({
    declarations: [LoginComponent, TranslateStubPipe],
    imports: [FormsModule],
    providers: [
        translateServiceProvider(),
        { provide: LoginService, useValue: loginSpy },
        { provide: NotificationService, useValue: notifSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: routeStub },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    loginService.isLoggedIn.and.returnValue(false);
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should skip login when the PWA session is already restored', () => {
    loginService.isLoggedIn.and.returnValue(true);
    component.ngOnInit();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
  });

  it('should have initial state', () => {
    expect(component.form.username).toBe('');
    expect(component.form.password).toBe('');
    expect(component.error).toBe('');
    expect(component.loading).toBeFalse();
  });

  describe('onSubmit', () => {
    it('should call login then getMe and navigate to dashboard', () => {
      loginService.login.and.returnValue(of({ accessToken: 'token123' }));
      loginService.getMe.and.returnValue(of({ id: 1, username: 'admin', roles: ['SYSTEM_ADMIN'] } as any));

      component.onSubmit();
      expect(component.loading).toBeFalse();
      expect(loginService.login).toHaveBeenCalledWith({ username: '', password: '' });
      expect(loginService.setCurrentUser).toHaveBeenCalled();
      expect(router.navigateByUrl).toHaveBeenCalledWith('/dashboard');
    });

    it('should set error on login failure', () => {
      loginService.login.and.returnValue(throwError(() => ({
        error: { message: 'Invalid credentials' }
      })));

      component.onSubmit();
      expect(component.error).toBe('Invalid credentials');
      expect(component.loading).toBeFalse();
    });

    it('should set default error message when no error message', () => {
      loginService.login.and.returnValue(throwError(() => ({ error: {} })));
      component.onSubmit();
      expect(component.error).toBe('AUTH.INVALID_CREDENTIALS');
    });

    it('should handle getMe failure and purge session', () => {
      loginService.login.and.returnValue(of({ accessToken: 'token123' }));
      loginService.getMe.and.returnValue(throwError(() => ({ error: {} })));

      component.onSubmit();
      expect(component.error).toBe('AUTH.USER_FETCH_ERROR');
      expect(loginService.purgeSession).toHaveBeenCalled();
      expect(router.navigateByUrl).not.toHaveBeenCalled();
    });
  });
});
