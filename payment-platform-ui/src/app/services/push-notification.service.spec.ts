// @ts-nocheck
/**
 * Tests du service PushNotificationService.
 * Perimetre : cas should be created without configured keys (push disabled); requestPermissionAndGetToken should resolve null when unconfigured; registerToken should POST the token (voir blocs describe/it).
 * Moyens : HttpTestingController, TestBed, client HTTP de test.
 */
import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { PushNotificationService } from './push-notification.service';

describe('PushNotificationService', () => {
  let service: PushNotificationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [],
      providers: [PushNotificationService, provideHttpClient(withInterceptorsFromDi()), provideHttpClientTesting()]
    });
    service = TestBed.inject(PushNotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created without configured keys (push disabled)', () => {
    expect(service).toBeTruthy();
    expect(PushNotificationService.isConfigured()).toBeFalse();
  });

  it('requestPermissionAndGetToken should resolve null when unconfigured', async () => {
    await expectAsync(service.requestPermissionAndGetToken()).toBeResolvedTo(null);
  });

  it('registerToken should POST the token', () => {
    service.registerToken('fcm-abc').subscribe();
    const req = httpMock.expectOne('/api/fcm-tokens');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ token: 'fcm-abc' });
    req.flush(null);
  });
});
