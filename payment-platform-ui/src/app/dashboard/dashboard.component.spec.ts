// @ts-nocheck
/**
 * Tests du composant DashboardComponent.
 * Perimetre : instanciation et blocs ngOnInit, getGreeting (voir blocs describe/it).
 * Moyens : TestBed + fixture, stubs jasmine, client HTTP de test.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { DashboardComponent } from './dashboard.component';
import { LoginService } from '../services/login.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('DashboardComponent', () => {
  let component: DashboardComponent;
  let fixture: ComponentFixture<DashboardComponent>;
  let loginService: jasmine.SpyObj<LoginService>;

  beforeEach(() => {
    const loginSpy = jasmine.createSpyObj('LoginService', ['getCurrentUser', 'hasRole']);

    TestBed.configureTestingModule({
      declarations: [DashboardComponent],
      imports: [],
      providers: [
        { provide: LoginService, useValue: loginSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ],
      schemas: [NO_ERRORS_SCHEMA]
    });
    fixture = TestBed.createComponent(DashboardComponent);
    component = fixture.componentInstance;
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load user and detect SYSTEM_ADMIN role', () => {
      loginService.getCurrentUser.and.returnValue({
        id: 1, username: 'admin', roles: ['SYSTEM_ADMIN']
      } as any);
      component.ngOnInit();
      expect(component.user).toBeTruthy();
      expect(component.activeRole).toBe('SYSTEM_ADMIN');
    });

    it('should detect SUPPLIER_ADMIN over other roles', () => {
      loginService.getCurrentUser.and.returnValue({
        id: 2, username: 'sup', roles: ['SUPPLIER_ADMIN', 'SHOP_AGENT']
      } as any);
      component.ngOnInit();
      expect(component.activeRole).toBe('SUPPLIER_ADMIN');
    });

    it('should detect SUPPLIER_AGENT', () => {
      loginService.getCurrentUser.and.returnValue({
        id: 3, username: 'agent', roles: ['SUPPLIER_AGENT']
      } as any);
      component.ngOnInit();
      expect(component.activeRole).toBe('SUPPLIER_AGENT');
    });

    it('should detect SHOP_ADMIN', () => {
      loginService.getCurrentUser.and.returnValue({
        id: 4, username: 'shop', roles: ['SHOP_ADMIN']
      } as any);
      component.ngOnInit();
      expect(component.activeRole).toBe('SHOP_ADMIN');
    });

    it('should detect SHOP_AGENT', () => {
      loginService.getCurrentUser.and.returnValue({
        id: 5, username: 'shopAgent', roles: ['SHOP_AGENT']
      } as any);
      component.ngOnInit();
      expect(component.activeRole).toBe('SHOP_AGENT');
    });

    it('should set activeRole to null when user has no roles', () => {
      loginService.getCurrentUser.and.returnValue({
        id: 6, username: 'nobody', roles: []
      } as any);
      component.ngOnInit();
      expect(component.activeRole).toBeNull();
    });

    it('should set user to null when getCurrentUser returns null', () => {
      loginService.getCurrentUser.and.returnValue(null as any);
      component.ngOnInit();
      expect(component.user).toBeNull();
      expect(component.activeRole).toBeNull();
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
});
