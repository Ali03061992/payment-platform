// @ts-nocheck
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { DashboardComponent } from './dashboard.component';
import { LoginService } from '../services/login.service';
import { UserService } from '../services/user.service';
import { of } from 'rxjs';
import { User } from '../models/user.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let loginService: jasmine.SpyObj<LoginService>;
  let userService: jasmine.SpyObj<UserService>;

  beforeEach(() => {
    const loginSpy = jasmine.createSpyObj('LoginService', ['getCurrentUser', 'hasRole']);
    const userSpy = jasmine.createSpyObj('UserService', ['list']);

    TestBed.configureTestingModule({
    declarations: [DashboardComponent],
    imports: [],
    providers: [
        { provide: LoginService, useValue: loginSpy },
        { provide: UserService, useValue: userSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
    userService = TestBed.inject(UserService) as jasmine.SpyObj<UserService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load user', () => {
      loginService.getCurrentUser.and.returnValue({ id: 1, username: 'admin' } as any);
      loginService.hasRole.and.returnValue(false);
      component.ngOnInit();
      expect(component.user).toBeTruthy();
    });

    it('should load stats for admin', () => {
      loginService.getCurrentUser.and.returnValue({ id: 1, username: 'admin' } as any);
      loginService.hasRole.and.returnValue(true);
      const mockUsers: User[] = [
        { id: 1, username: 'u1', email: '', firstName: '', lastName: '', phone: '', organizationId: 1, roles: ['SYSTEM_ADMIN'], status: 'ACTIVE', createdAt: '', updatedAt: '' },
        { id: 2, username: 'u2', email: '', firstName: '', lastName: '', phone: '', organizationId: 2, roles: ['SUPPLIER_ADMIN'], status: 'DISABLED', createdAt: '', updatedAt: '' },
        { id: 3, username: 'u3', email: '', firstName: '', lastName: '', phone: '', organizationId: 3, roles: ['SHOP_AGENT'], status: 'ACTIVE', createdAt: '', updatedAt: '' }
      ];
      userService.list.and.returnValue(of(mockUsers));
      component.ngOnInit();
      expect(component.stats.totalUsers).toBe(3);
      expect(component.stats.activeUsers).toBe(2);
      expect(component.stats.disabledUsers).toBe(1);
      expect(component.stats.suppliers).toBe(1);
      expect(component.stats.shops).toBe(1);
    });
  });

  describe('getGreeting', () => {
    it('should return a greeting string', () => {
      const greeting = component.getGreeting();
      expect(greeting).toBeTruthy();
    });

    it('should return one of the known greetings', () => {
      const validGreetings = ['Bonjour', 'Bon après-midi', 'Bonsoir'];
      const greeting = component.getGreeting();
      expect(validGreetings).toContain(greeting);
    });
  });

  describe('ngOnInit', () => {
    it('should not load stats when not admin', () => {
      loginService.getCurrentUser.and.returnValue({ id: 1, username: 'user' } as any);
      loginService.hasRole.and.returnValue(false);
      component.ngOnInit();
      expect(userService.list).not.toHaveBeenCalled();
    });

    it('should set user to null when getCurrentUser returns null', () => {
      loginService.getCurrentUser.and.returnValue(null as any);
      loginService.hasRole.and.returnValue(false);
      component.ngOnInit();
      expect(component.user).toBeNull();
    });
  });
});
