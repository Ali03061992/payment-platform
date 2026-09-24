// @ts-nocheck
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LoginService } from './login.service';
import { LoginResponse, User } from '../models/user.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('LoginService', () => {
  let service: LoginService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    TestBed.configureTestingModule({
    imports: [],
    providers: [LoginService, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
});
    service = TestBed.inject(LoginService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    it('should POST to login and store token', () => {
      const mockResponse: LoginResponse = { accessToken: 'jwt-token-123' };
      service.login({ username: 'admin', password: 'pass' }).subscribe(res => {
        expect(res.accessToken).toBe('jwt-token-123');
        expect(sessionStorage.getItem('token')).toBe('jwt-token-123');
      });
      const req = httpMock.expectOne('/api/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ username: 'admin', password: 'pass' });
      req.flush(mockResponse);
    });

    it('should store refresh token when provided', () => {
      const mockResponse: LoginResponse = { accessToken: 'jwt', refreshToken: 'ref-123', refreshExpiresIn: 604800 };
      service.login({ username: 'admin', password: 'pass' }).subscribe();
      const req = httpMock.expectOne('/api/auth/login');
      req.flush(mockResponse);
      expect(sessionStorage.getItem('refreshToken')).toBe('ref-123');
    });
  });

  describe('refresh', () => {
    it('should POST stored refresh token and rotate stored pair', () => {
      sessionStorage.setItem('token', 'old-access');
      sessionStorage.setItem('refreshToken', 'old-refresh');
      service.refresh().subscribe(res => {
        expect(res.accessToken).toBe('new-access');
        expect(sessionStorage.getItem('token')).toBe('new-access');
        expect(sessionStorage.getItem('refreshToken')).toBe('new-refresh');
      });
      const req = httpMock.expectOne('/api/auth/refresh');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: 'old-refresh' });
      req.flush({ accessToken: 'new-access', tokenType: 'Bearer', expiresIn: 1800, refreshToken: 'new-refresh', refreshExpiresIn: 604800 });
    });

    it('should error when no refresh token stored', () => {
      service.refresh().subscribe({
        error: (e) => expect(e.message).toBe('No refresh token')
      });
      httpMock.expectNone('/api/auth/refresh');
    });
  });

  describe('getMe', () => {
    it('should GET current user', () => {
      const mockUser: User = {
        id: 1, username: 'admin', email: 'a@b.com', firstName: 'A', lastName: 'B',
        phone: '123', organizationId: 1, roles: ['SYSTEM_ADMIN'], status: 'ACTIVE',
        createdAt: '', updatedAt: ''
      };
      service.getMe().subscribe(user => {
        expect(user.username).toBe('admin');
      });
      const req = httpMock.expectOne('/api/auth/me');
      expect(req.request.method).toBe('GET');
      req.flush(mockUser);
    });
  });

  describe('logout', () => {
    it('should remove token and user from sessionStorage', () => {
      sessionStorage.setItem('token', 'test-token');
      sessionStorage.setItem('user', '{"id":1}');
      service.logout();
      expect(sessionStorage.getItem('token')).toBeNull();
      expect(sessionStorage.getItem('user')).toBeNull();
    });

    it('should revoke server-side refresh token best-effort', () => {
      sessionStorage.setItem('token', 'test-token');
      sessionStorage.setItem('refreshToken', 'ref-1');
      sessionStorage.setItem('user', '{"id":1}');
      service.logout();
      const req = httpMock.expectOne('/api/auth/logout');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ refreshToken: 'ref-1' });
      req.flush(null);
      expect(sessionStorage.getItem('refreshToken')).toBeNull();
    });
  });

  describe('isLoggedIn', () => {
    it('should return true when token exists', () => {
      sessionStorage.setItem('token', 'test-token');
      expect(service.isLoggedIn()).toBeTrue();
    });

    it('should return false when no token', () => {
      expect(service.isLoggedIn()).toBeFalse();
    });
  });

  describe('getCurrentUser', () => {
    it('should return user when stored', () => {
      const user = { id: 1, username: 'admin', email: 'a@b.com', firstName: 'A', lastName: 'B', phone: '123', organizationId: 1, roles: ['SYSTEM_ADMIN'], status: 'ACTIVE' as const, createdAt: '', updatedAt: '' };
      sessionStorage.setItem('user', JSON.stringify(user));
      expect(service.getCurrentUser()).toEqual(user);
    });

    it('should return null when no user stored', () => {
      expect(service.getCurrentUser()).toBeNull();
    });
  });

  describe('hasRole', () => {
    it('should return true when user has matching role', () => {
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN'] }));
      expect(service.hasRole('SYSTEM_ADMIN')).toBeTrue();
    });

    it('should return true when user has one of multiple roles', () => {
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SHOP_AGENT'] }));
      expect(service.hasRole('SHOP_ADMIN', 'SHOP_AGENT')).toBeTrue();
    });

    it('should return false when user does not have matching role', () => {
      sessionStorage.setItem('user', JSON.stringify({ roles: ['SHOP_AGENT'] }));
      expect(service.hasRole('SYSTEM_ADMIN')).toBeFalse();
    });

    it('should return false when no user', () => {
      expect(service.hasRole('SYSTEM_ADMIN')).toBeFalse();
    });
  });
});
