// @ts-nocheck
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { PasswordSetupComponent } from './password-setup.component';
import { ToastService } from '../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('PasswordSetupComponent', () => {
  let component: PasswordSetupComponent;
  let fixture: ComponentFixture<PasswordSetupComponent>;
  let httpMock: HttpTestingController;
  let toast: jasmine.SpyObj<ToastService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const toastSpy = jasmine.createSpyObj('ToastService', ['error', 'success']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    TestBed.configureTestingModule({
    declarations: [PasswordSetupComponent],
    imports: [FormsModule],
    providers: [
        { provide: ToastService, useValue: toastSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({ token: 'valid-token' }) } } },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(PasswordSetupComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should validate token on init', () => {
      component.ngOnInit();
      const req = httpMock.expectOne('/api/auth/password-setup/validate?token=valid-token');
      expect(req.request.method).toBe('GET');
      req.flush({ valid: true });
      expect(component.tokenValid).toBeTrue();
      expect(component.checkingToken).toBeFalse();
    });

    it('should handle invalid token', () => {
      component.ngOnInit();
      const req = httpMock.expectOne('/api/auth/password-setup/validate?token=valid-token');
      req.flush({ valid: false });
      expect(component.tokenValid).toBeFalse();
      expect(toast.error).toHaveBeenCalled();
    });

    it('should handle error during validation', () => {
      component.ngOnInit();
      const req = httpMock.expectOne('/api/auth/password-setup/validate?token=valid-token');
      req.error(new ProgressEvent('error'));
      expect(component.checkingToken).toBeFalse();
      expect(toast.error).toHaveBeenCalled();
    });
  });

  describe('onSubmit', () => {
    it('should show error for short password', () => {
      component.token = 'token';
      component.newPassword = 'short';
      component.confirmPassword = 'short';
      component.onSubmit();
      expect(toast.error).toHaveBeenCalledWith('Le mot de passe doit contenir au moins 8 caractères.');
    });

    it('should show error when passwords do not match', () => {
      component.token = 'token';
      component.newPassword = 'longpassword';
      component.confirmPassword = 'different';
      component.onSubmit();
      expect(toast.error).toHaveBeenCalledWith('Les mots de passe ne correspondent pas.');
    });

    it('should submit when passwords match and are long enough', () => {
      component.token = 'token';
      component.newPassword = 'longpassword';
      component.confirmPassword = 'longpassword';
      component.onSubmit();

      const req = httpMock.expectOne('/api/auth/password-setup/complete');
      expect(req.request.method).toBe('POST');
      req.flush({ message: 'OK' });
      expect(component.success).toBeTrue();
      expect(component.loading).toBeFalse();
    });

    it('should handle submission error', () => {
      component.token = 'token';
      component.newPassword = 'longpassword';
      component.confirmPassword = 'longpassword';
      component.onSubmit();

      const req = httpMock.expectOne('/api/auth/password-setup/complete');
      req.error(new ErrorEvent('error'), { statusText: 'Network error' });
      expect(component.loading).toBeFalse();
    });
  });

  describe('goToLogin', () => {
    it('should navigate to login', () => {
      component.goToLogin();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });
});

describe('PasswordSetupComponent (no token)', () => {
  let component: PasswordSetupComponent;
  let fixture: ComponentFixture<PasswordSetupComponent>;
  let httpMock: HttpTestingController;
  let toast: jasmine.SpyObj<ToastService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const toastSpy = jasmine.createSpyObj('ToastService', ['error', 'success']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    TestBed.configureTestingModule({
    declarations: [PasswordSetupComponent],
    imports: [FormsModule],
    providers: [
        { provide: ToastService, useValue: toastSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: convertToParamMap({}) } } },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(PasswordSetupComponent);
    component = fixture.componentInstance;
    httpMock = TestBed.inject(HttpTestingController);
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should show error when no token', () => {
    component.ngOnInit();
    expect(toast.error).toHaveBeenCalledWith('Lien invalide. Veuillez demander un nouveau lien de configuration.');
    expect(component.checkingToken).toBeFalse();
    expect(component.token).toBe('');
  });
});
