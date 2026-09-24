// @ts-nocheck
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { NotFoundComponent } from './not-found.component';
import { LoginService } from '../../services/login.service';

describe('NotFoundComponent', () => {
  let component: NotFoundComponent;
  let fixture: ComponentFixture<NotFoundComponent>;
  let router: jasmine.SpyObj<Router>;
  let loginService: jasmine.SpyObj<LoginService>;

  beforeEach(() => {
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    const loginSpy = jasmine.createSpyObj('LoginService', ['isLoggedIn']);
    TestBed.configureTestingModule({
      declarations: [NotFoundComponent],
      providers: [
        { provide: Router, useValue: routerSpy },
        { provide: LoginService, useValue: loginSpy }
      ]
    });
    fixture = TestBed.createComponent(NotFoundComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display 404 content', () => {
    const el: HTMLElement = fixture.nativeElement;
    expect(el.textContent).toContain('404');
    expect(el.textContent).toContain('introuvable');
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
