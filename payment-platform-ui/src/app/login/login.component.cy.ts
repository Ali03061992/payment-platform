/// <reference types="cypress" />
/// <reference types="@cypress/angular" />

import { LoginComponent } from './login.component';
import { LoginService } from '../services/login.service';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

describe('LoginComponent', () => {
  let loginServiceSpy: jasmine.SpyObj<LoginService>;
  let routerSpy: jasmine.SpyObj<Router>;

  beforeEach(() => {
    loginServiceSpy = jasmine.createSpyObj('LoginService', ['login', 'getMe']);
    routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    
    // Clear localStorage before each test
    localStorage.clear();
  });

  const mountComponent = (overrides: Partial<{ login: any; getMe: any }> = {}) => {
    const loginReturn = overrides.login ?? of({ accessToken: 'mock-jwt-token' });
    const getMeReturn = overrides.getMe ?? of({ 
      id: 1, 
      username: 'testuser', 
      roles: ['SYSTEM_ADMIN'], 
      organizationId: null 
    });

    loginServiceSpy.login.and.returnValue(loginReturn);
    loginServiceSpy.getMe.and.returnValue(getMeReturn);

    return cy.mount(LoginComponent, {
      providers: [
        { provide: LoginService, useValue: loginServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });
  };

  it('should render login form with username and password fields', () => {
    mountComponent();
    cy.get('input[name="username"]').should('exist');
    cy.get('input[name="password"]').should('exist');
    cy.get('button[type="submit"]').should('exist');
  });

  it('should show error when credentials are invalid', () => {
    mountComponent({
      login: throwError(() => ({ error: { message: 'Identifiants invalides' } }))
    });
    
    cy.get('input[name="username"]').type('wronguser');
    cy.get('input[name="password"]').type('wrongpass');
    cy.get('button[type="submit"]').click();
    
    cy.get('.error, .alert-error, [class*="error"]').should('contain', 'Identifiants invalides');
  });

  it('should call loginService and navigate on successful login', () => {
    mountComponent();
    
    cy.get('input[name="username"]').type('system.admin');
    cy.get('input[name="password"]').type('Admin@123');
    cy.get('button[type="submit"]').click();
    
    cy.wrap(null).should(() => {
      expect(loginServiceSpy.login).toHaveBeenCalledWith({ username: 'system.admin', password: 'Admin@123' });
      expect(loginServiceSpy.getMe).toHaveBeenCalled();
      expect(localStorage.getItem('token')).to.equal('mock-jwt-token');
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
    });
  });

  it('should fallback to mock user when getMe fails', () => {
    mountComponent({
      getMe: throwError(() => new Error('Failed'))
    });
    
    cy.get('input[name="username"]').type('system.admin');
    cy.get('input[name="password"]').type('Admin@123');
    cy.get('button[type="submit"]').click();
    
    cy.wrap(null).should(() => {
      expect(localStorage.getItem('user')).to.exist;
      const user = JSON.parse(localStorage.getItem('user')!);
      expect(user.username).to.equal('system.admin');
      expect(user.roles).to.deep.equal(['SYSTEM_ADMIN']);
      expect(routerSpy.navigate).toHaveBeenCalledWith(['/dashboard']);
    });
  });

  it('should disable button and show loading state during submission', () => {
    // Use a delayed observable to test loading state
    let resolveLogin: (value: any) => void;
    const loginPromise = new Promise(resolve => { resolveLogin = resolve; });
    
    loginServiceSpy.login.and.returnValue(
      new Observable(observer => {
        loginPromise.then(val => {
          observer.next(val);
          observer.complete();
        });
      })
    );
    loginServiceSpy.getMe.and.returnValue(of({ 
      id: 1, username: 'test', roles: ['SHOP_ADMIN'], organizationId: 3 
    }));

    cy.mount(LoginComponent, {
      providers: [
        { provide: LoginService, useValue: loginServiceSpy },
        { provide: Router, useValue: routerSpy },
      ],
    });
    
    cy.get('input[name="username"]').type('shopuser');
    cy.get('input[name="password"]').type('Admin@123');
    cy.get('button[type="submit"]').click();
    
    cy.get('button[type="submit"]').should('be.disabled');
    cy.get('button[type="submit"]').should('contain', 'Chargement');
    
    // Resolve the promise
    resolveLogin!({ accessToken: 'delayed-token' });
    
    cy.wrap(null).should(() => {
      expect(loginServiceSpy.getMe).toHaveBeenCalled();
    });
  });
});

// Need to import Observable for the delayed test
import { Observable } from 'rxjs';