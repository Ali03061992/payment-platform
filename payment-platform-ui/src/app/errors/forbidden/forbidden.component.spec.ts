// @ts-nocheck
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { ForbiddenComponent } from './forbidden.component';
import { LoginService } from '../../services/login.service';

describe('ForbiddenComponent', () => {
  let component: ForbiddenComponent;
  let fixture: ComponentFixture<ForbiddenComponent>;
  let router: jasmine.SpyObj<Router>;
  let loginService: jasmine.SpyObj<LoginService>;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const loginSpy = jasmine.createSpyObj('LoginService', ['isLoggedIn']);
    TestBed.configureTestingModule({
      declarations: [ForbiddenComponent],
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: LoginService, useValue: loginSpy }
      ]
    });
    fixture = TestBed.createComponent(ForbiddenComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display 403 content', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('403');
    expect(el.textContent).toContain('Accès refusé');
  });

  it('should go to dashboard when logged in', () => {
    loginService.isLoggedIn.and.returnValue(true);
    component.back();
    expect(router.navigate).toHaveBeenCalledWith(['/dashboard']);
  });

  it('should go to login when anonymous', () => {
    loginService.isLoggedIn.and.returnValue(false);
    component.back();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
