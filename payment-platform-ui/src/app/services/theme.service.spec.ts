// @ts-nocheck
/**
 * Tests ThemeService : relecture des accents depuis le backend au refresh.
 */
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { BehaviorSubject, of } from 'rxjs';
import { ThemeService } from './theme.service';
import { LoginService } from './login.service';
import { UserPreferencesService } from './user-preferences.service';

describe('ThemeService backend accents', () => {
  let service: ThemeService;
  let httpMock: HttpTestingController;
  let user$: BehaviorSubject<any>;

  beforeEach(() => {
    localStorage.clear();
    user$ = new BehaviorSubject<any>({ id: 'u1' });
    const loginStub = { getCurrentUser: () => ({ id: 'u1' }), currentUser$: user$.asObservable() };
    TestBed.configureTestingModule({
      providers: [
        ThemeService,
        UserPreferencesService,
        { provide: LoginService, useValue: loginStub },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
      ]
    });
    service = TestBed.inject(ThemeService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('should fetch backend accents on init and apply them', () => {
    const req = httpMock.expectOne('/api/users/me/preferences');
    expect(req.request.method).toBe('GET');
    req.flush({ accentColor1: '#123456', accentColor2: '#654321' });
    expect(service.currentAccent1).toBe('#123456');
    expect(service.currentAccent2).toBe('#654321');
  });

  it('should fetch only once per user session', () => {
    httpMock.expectOne('/api/users/me/preferences').flush({ accentColor1: '#123456', accentColor2: '#654321' });
    user$.next({ id: 'u1' });
    user$.next({ id: 'u1' });
    httpMock.expectNone('/api/users/me/preferences');
  });

  it('should keep defaults when backend has no accents', () => {
    httpMock.expectOne('/api/users/me/preferences').flush({});
    expect(service.currentAccent1).toBe('#0284c7');
    expect(service.currentAccent2).toBe('#e63946');
  });
});
