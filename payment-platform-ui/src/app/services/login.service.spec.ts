// @ts-nocheck
/**
 * Tests du service LoginService.
 * Perimetre : instanciation et blocs login, refresh (cookie/migration), restoreSession,
 * getMe, logout, isLoggedIn, getCurrentUser, hasRole.
 * Moyens : HttpTestingController, TestBed, client HTTP de test.
 */
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { LoginService } from './login.service';
import { LoginResponse, User } from '../models/user.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

/** JWT factice avec exp n secondes dans le passé/futur (base64url du payload). */
function makeJwt(expSeconds: number): string {
  const payload = btoa(JSON.stringify({ exp: expSeconds }));
  return `header.${payload}.sig`;
}

describe('LoginService', () => {
  let service: LoginService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    sessionStorage.clear();
    localStorage.clear();
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
    localStorage.clear();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('login', () => {
    it('should POST to login and keep token in memory only', () => {
      const mockResponse: LoginResponse = { accessToken: 'jwt-token-123' };
      service.login({ username: 'admin', password: 'pass' }).subscribe(res => {
        expect(res.accessToken).toBe('jwt-token-123');
      });
      const req = httpMock.expectOne('/api/auth/login');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ username: 'admin', password: 'pass' });
      req.flush(mockResponse);
      // Access token en mémoire, jamais dans sessionStorage (cookie HttpOnly pour le refresh).
      expect(service.getToken()).toBe('jwt-token-123');
      expect(sessionStorage.getItem('token')).toBeNull();
    });

    it('should not persist refresh token (cookie HttpOnly backend)', () => {
      const mockResponse: LoginResponse = { accessToken: 'jwt', refreshToken: 'ref-123', refreshExpiresIn: 604800 };
      service.login({ username: 'admin', password: 'pass' }).subscribe();
      const req = httpMock.expectOne('/api/auth/login');
      req.flush(mockResponse);
      expect(sessionStorage.getItem('refreshToken')).toBeNull();
    });

    it('should purge legacy sessionStorage keys on adopt', () => {
      sessionStorage.setItem('token', 'legacy');
      sessionStorage.setItem('refreshToken', 'legacy-ref');
      service.login({ username: 'admin', password: 'pass' }).subscribe();
      const req = httpMock.expectOne('/api/auth/login');
      req.flush({ accessToken: 'new-access' });
      expect(sessionStorage.getItem('token')).toBeNull();
      expect(sessionStorage.getItem('refreshToken')).toBeNull();
    });
  });

  describe('refresh', () => {
    it('should POST empty body (cookie HttpOnly) and store token in memory', () => {
      service.refresh().subscribe(token => {
        expect(token).toBe('new-access');
        expect(service.getToken()).toBe('new-access');
      });
      const req = httpMock.expectOne('/api/auth/refresh');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush({ accessToken: 'new-access', tokenType: 'Bearer', expiresIn: 1800, refreshToken: 'new-rt', refreshExpiresIn: 604800 });
      expect(sessionStorage.getItem('refreshToken')).toBeNull();
    });

    it('should send legacy refreshToken in body once (migration pre-cookie)', () => {
      sessionStorage.setItem('refreshToken', 'legacy-refresh');
      service.refresh().subscribe(token => {
        expect(token).toBe('new-access');
      });
      const req = httpMock.expectOne('/api/auth/refresh');
      expect(req.request.body).toEqual({ refreshToken: 'legacy-refresh' });
      req.flush({ accessToken: 'new-access', refreshToken: 'rotated' });
      // Migration consommée : la clé legacy est purgée après succès.
      expect(sessionStorage.getItem('refreshToken')).toBeNull();
    });

    it('should purge legacy key even on failure', () => {
      sessionStorage.setItem('refreshToken', 'dead-refresh');
      service.refresh().subscribe({ error: () => {} });
      const req = httpMock.expectOne('/api/auth/refresh');
      req.flush({ message: 'nope' }, { status: 401, statusText: 'Unauthorized' });
      expect(sessionStorage.getItem('refreshToken')).toBeNull();
    });

    it('should share single-flight between concurrent callers', () => {
      service.refresh().subscribe();
      service.refresh().subscribe();
      const req = httpMock.expectOne('/api/auth/refresh');
      req.flush({ accessToken: 'shared' });
      expect(service.getToken()).toBe('shared');
    });
  });

  describe('restoreSession', () => {
    it('should refresh via cookie then GET /me and persist user', () => {
      const user: User = {
        id: 'u1', username: 'admin', email: 'a@b.com', firstName: 'A', lastName: 'B',
        phone: '123', organizationId: 'org1', roles: ['SYSTEM_ADMIN'], status: 'ACTIVE',
        createdAt: '', updatedAt: ''
      };
      let restored: boolean | undefined;
      service.restoreSession().subscribe(v => restored = v);

      const refreshReq = httpMock.expectOne('/api/auth/refresh');
      expect(refreshReq.request.body).toEqual({});
      refreshReq.flush({ accessToken: 'boot-access' });

      const meReq = httpMock.expectOne('/api/auth/me');
      meReq.flush(user);

      expect(restored).toBeTrue();
      expect(service.isLoggedIn()).toBeFalse(); // boot-access n'a pas de exp valide
      expect(service.getToken()).toBe('boot-access');
      expect(service.getCurrentUser()?.id).toBe('u1');
      expect(sessionStorage.getItem('user')).toContain('u1'); // miroir re-synchrone
    });

    it('should resolve false when refresh fails (cookie absent/expiré)', () => {
      let restored: boolean | undefined;
      service.restoreSession().subscribe(v => restored = v);
      const req = httpMock.expectOne('/api/auth/refresh');
      req.flush({ message: 'no' }, { status: 401, statusText: 'Unauthorized' });
      expect(restored).toBeFalse();
      expect(service.getToken()).toBeNull();
    });

    it('should return true immediately when token already in memory', () => {
      service.adoptSession({ accessToken: makeJwt(Math.floor(Date.now() / 1000) + 3600) });
      let restored: boolean | undefined;
      service.restoreSession().subscribe(v => restored = v);
      expect(restored).toBeTrue();
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
    it('should purge memory and both storages', () => {
      service.adoptSession({ accessToken: 'test-token' });
      service.setCurrentUser({ id: 1 } as User);
      service.logout();
      expect(service.getToken()).toBeNull();
      expect(localStorage.getItem('user')).toBeNull();
      expect(sessionStorage.getItem('user')).toBeNull();
      // Best-effort réseau : la requête logout doit être consommée par le test.
      httpMock.expectOne('/api/auth/logout').flush(null);
    });

    it('should revoke server-side best-effort with empty body (cookie)', () => {
      service.adoptSession({ accessToken: 'test-token' });
      service.logout();
      const req = httpMock.expectOne('/api/auth/logout');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({});
      req.flush(null);
    });
  });

  describe('isLoggedIn', () => {
    it('should return true when in-memory token is unexpired', () => {
      service.adoptSession({ accessToken: makeJwt(Math.floor(Date.now() / 1000) + 3600) });
      expect(service.isLoggedIn()).toBeTrue();
    });

    it('should return false when token expired', () => {
      service.adoptSession({ accessToken: makeJwt(Math.floor(Date.now() / 1000) - 60) });
      expect(service.isLoggedIn()).toBeFalse();
    });

    it('should return false when no token', () => {
      expect(service.isLoggedIn()).toBeFalse();
    });
  });

  describe('getCurrentUser', () => {
    it('should return user when stored', () => {
      const user = { id: 1, username: 'admin', email: 'a@b.com', firstName: 'A', lastName: 'B', phone: '123', organizationId: 1, roles: ['SYSTEM_ADMIN'], status: 'ACTIVE' as const, createdAt: '', updatedAt: '' };
      localStorage.setItem('user', JSON.stringify(user));
      expect(service.getCurrentUser()).toEqual(user);
    });

    it('should return null when no user stored', () => {
      expect(service.getCurrentUser()).toBeNull();
    });
  });

  describe('updateCurrentUser', () => {
    it('should merge patch into stored user', () => {
      service.setCurrentUser({ id: 'u1', roles: ['SHOP_AGENT'], tourSeen: false } as unknown as User);
      service.updateCurrentUser({ tourSeen: true });
      expect(service.getCurrentUser()?.tourSeen).toBeTrue();
    });
  });

  describe('hasRole', () => {
    it('should return true when user has matching role', () => {
      localStorage.setItem('user', JSON.stringify({ roles: ['SYSTEM_ADMIN', 'SUPPLIER_ADMIN'] }));
      expect(service.hasRole('SYSTEM_ADMIN')).toBeTrue();
    });

    it('should return true when user has one of multiple roles', () => {
      localStorage.setItem('user', JSON.stringify({ roles: ['SHOP_AGENT'] }));
      expect(service.hasRole('SHOP_ADMIN', 'SHOP_AGENT')).toBeTrue();
    });

    it('should return false when user does not have matching role', () => {
      localStorage.setItem('user', JSON.stringify({ roles: ['SHOP_AGENT'] }));
      expect(service.hasRole('SYSTEM_ADMIN')).toBeFalse();
    });

    it('should return false when no user', () => {
      expect(service.hasRole('SYSTEM_ADMIN')).toBeFalse();
    });
  });
});
