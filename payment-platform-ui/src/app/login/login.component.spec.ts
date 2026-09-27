// @ts-nocheck
/**
 * Tests du composant LoginComponent.
 * Perimetre : instanciation et blocs onSubmit (voir blocs describe/it).
 * Moyens : TestBed + fixture, stubs jasmine, client HTTP de test, observables RxJS mockes.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { LoginComponent } from './login.component';
import { LoginService } from '../services/login.service';
import { NotificationService } from '../services/notification.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let loginService: jasmine.SpyObj<LoginService>;
  let notificationService: jasmine.SpyObj<NotificationService>;

  beforeEach(() => {
    const loginSpy = jasmine.createSpyObj('LoginService', ['login', 'getMe', 'logout']);
    const notifSpy = jasmine.createSpyObj('NotificationService', ['requestPermission']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
    declarations: [LoginComponent],
    imports: [FormsModule],
    providers: [
        { provide: LoginService, useValue: loginSpy },
        { provide: NotificationService, useValue: notifSpy },
        { provide: Router, useValue: routerSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
    notificationService = TestBed.inject(NotificationService) as jasmine.SpyObj<NotificationService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial state', () => {
    expect(component.form.username).toBe('');
    expect(component.form.password).toBe('');
    expect(component.error).toBe('');
    expect(component.loading).toBeFalse();
  });

  describe('onSubmit', () => {
    it('should call login and getMe on success', () => {
      loginService.login.and.returnValue(of({ accessToken: 'token123' }));
      loginService.getMe.and.returnValue(of({ id: 1, username: 'admin', roles: ['SYSTEM_ADMIN'] } as any));

      component.onSubmit();
      expect(component.loading).toBeFalse();
      expect(loginService.login).toHaveBeenCalledWith({ username: '', password: '' });
      expect(sessionStorage.getItem('token')).toBe('token123');
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
      expect(component.error).toBe('Identifiants invalides');
    });

    it('should handle getMe failure', () => {
      loginService.login.and.returnValue(of({ accessToken: 'token123' }));
      loginService.getMe.and.returnValue(throwError(() => ({ error: {} })));

      component.onSubmit();
      expect(component.error).toBe('Impossible de récupérer les informations utilisateur');
      expect(sessionStorage.getItem('token')).toBeNull();
    });
  });
});
