// @ts-nocheck
/**
 * Tests du service AuthService.
 * Perimetre : instanciation et changement de mot de passe (voir blocs describe/it).
 * Moyens : HttpTestingController, TestBed, client HTTP de test.
 */
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
    imports: [],
    providers: [AuthService, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
});
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('changePassword', () => {
    it('should POST to change-password', () => {
      service.changePassword('OldPass@1', 'NewPass@1').subscribe();
      const req = httpMock.expectOne('/api/auth/change-password');
      expect(req.request.method).toBe('POST');
      expect(req.request.body).toEqual({ currentPassword: 'OldPass@1', newPassword: 'NewPass@1' });
      req.flush(null);
    });
  });
});
