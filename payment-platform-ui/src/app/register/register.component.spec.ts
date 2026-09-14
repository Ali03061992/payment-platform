import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { RegisterComponent } from './register.component';
import { AuthService } from '../services/auth.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('RegisterComponent', () => {
  let component: RegisterComponent;
  let fixture: ComponentFixture<RegisterComponent>;
  let authService: jasmine.SpyObj<AuthService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(() => {
    const authSpy = jasmine.createSpyObj('AuthService', ['register']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    TestBed.configureTestingModule({
    declarations: [RegisterComponent],
    imports: [FormsModule],
    providers: [
        { provide: AuthService, useValue: authSpy },
        { provide: Router, useValue: routerSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(RegisterComponent);
    component = fixture.componentInstance;
    authService = TestBed.inject(AuthService) as jasmine.SpyObj<AuthService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have initial state', () => {
    expect(component.form.username).toBe('');
    expect(component.form.role).toBe('SHOP_AGENT');
    expect(component.error).toBe('');
    expect(component.success).toBeFalse();
    expect(component.loading).toBeFalse();
  });

  it('should have roles array', () => {
    expect(component.roles.length).toBe(4);
    expect(component.roles[0].value).toBe('SUPPLIER_ADMIN');
  });

  describe('onSubmit', () => {
    it('should call register and set success', () => {
      authService.register.and.returnValue(of({} as any));
      component.onSubmit();
      expect(component.success).toBeTrue();
      expect(component.loading).toBeFalse();
      expect(authService.register).toHaveBeenCalled();
    });

    it('should set error on failure', () => {
      authService.register.and.returnValue(throwError(() => ({
        error: { message: 'Username taken' }
      })));
      component.onSubmit();
      expect(component.error).toBe('Username taken');
      expect(component.loading).toBeFalse();
    });

    it('should set default error when no message', () => {
      authService.register.and.returnValue(throwError(() => ({ error: {} })));
      component.onSubmit();
      expect(component.error).toBe("Erreur lors de l'inscription");
    });
  });
});
