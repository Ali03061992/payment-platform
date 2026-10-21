// @ts-nocheck
/**
 * Tests du composant NotificationBannerComponent.
 * Perimetre : instanciation et blocs ngOnInit, acceptNotifications, dismissNotifications (voir blocs describe/it).
 * Moyens : TestBed + fixture, stubs jasmine.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { NotificationBannerComponent } from './notification-banner.component';
import { NotificationService } from '../../services/notification.service';
import { PushNotificationService } from '../../services/push-notification.service';
import { WebPushService } from '../../services/web-push.service';
import { TranslateStubPipe } from '../../testing/translate-stubs';

describe('NotificationBannerComponent', () => {
  let component: NotificationBannerComponent;
  let fixture: ComponentFixture<NotificationBannerComponent>;
  let notificationService: jasmine.SpyObj<NotificationService>;
  let pushService: jasmine.SpyObj<PushNotificationService>;
  let webPushService: jasmine.SpyObj<WebPushService>;

  beforeEach(() => {
    const notifSpy = jasmine.createSpyObj('NotificationService', ['getPermissionStatus', 'requestPermission', 'showBrowserNotification']);
    notifSpy.getPermissionStatus.and.returnValue('default');
    const pushSpy = jasmine.createSpyObj('PushNotificationService', ['requestPermissionAndGetToken', 'listenToMessages']);
    pushSpy.requestPermissionAndGetToken.and.returnValue(Promise.resolve('fcm-token'));
    const webPushSpy = jasmine.createSpyObj('WebPushService', ['isSupported', 'enable']);
    webPushSpy.isSupported.and.returnValue(false);
    webPushSpy.enable.and.returnValue(of(void 0));
    localStorage.clear();

    TestBed.configureTestingModule({
      declarations: [NotificationBannerComponent, TranslateStubPipe],
      providers: [
        { provide: NotificationService, useValue: notifSpy },
        { provide: PushNotificationService, useValue: pushSpy },
        { provide: WebPushService, useValue: webPushSpy }
      ]
    });
    fixture = TestBed.createComponent(NotificationBannerComponent);
    component = fixture.componentInstance;
    notificationService = TestBed.inject(NotificationService) as jasmine.SpyObj<NotificationService>;
    pushService = TestBed.inject(PushNotificationService) as jasmine.SpyObj<PushNotificationService>;
    webPushService = TestBed.inject(WebPushService) as jasmine.SpyObj<WebPushService>;
  });

  afterEach(() => {
    localStorage.clear();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should show banner when no user choice and default permission', () => {
      notificationService.getPermissionStatus.and.returnValue('default');
      component.ngOnInit();
      expect(component.showBanner).toBeTrue();
    });

    it('should not show banner when unsupported', () => {
      notificationService.getPermissionStatus.and.returnValue('unsupported');
      component.ngOnInit();
      expect(component.showBanner).toBeFalse();
    });

    it('should not show banner when already granted', () => {
      notificationService.getPermissionStatus.and.returnValue('granted');
      component.ngOnInit();
      expect(component.showBanner).toBeFalse();
    });

    it('should not show banner when user already chose', () => {
      localStorage.setItem('notification_choice', 'accepted');
      notificationService.getPermissionStatus.and.returnValue('default');
      component.ngOnInit();
      expect(component.showBanner).toBeFalse();
    });
  });

  describe('acceptNotifications', () => {
    it('should request permission and hide banner', async () => {
      notificationService.requestPermission.and.returnValue(Promise.resolve('granted'));
      component.showBanner = true;
      await component.acceptNotifications();
      expect(component.showBanner).toBeFalse();
      expect(localStorage.getItem('notification_choice')).toBe('accepted');
    });

    it('should subscribe Web Push when supported', async () => {
      notificationService.requestPermission.and.returnValue(Promise.resolve('granted'));
      webPushService.isSupported.and.returnValue(true);
      component.showBanner = true;
      await component.acceptNotifications();
      expect(webPushService.enable).toHaveBeenCalled();
      expect(pushService.requestPermissionAndGetToken).not.toHaveBeenCalled();
    });

    it('should fall back to FCM when Web Push is unsupported', async () => {
      notificationService.requestPermission.and.returnValue(Promise.resolve('granted'));
      webPushService.isSupported.and.returnValue(false);
      component.showBanner = true;
      await component.acceptNotifications();
      expect(pushService.requestPermissionAndGetToken).toHaveBeenCalled();
      expect(pushService.listenToMessages).toHaveBeenCalled();
    });

    it('should not register FCM token when permission denied', async () => {
      notificationService.requestPermission.and.returnValue(Promise.resolve('denied'));
      component.showBanner = true;
      await component.acceptNotifications();
      expect(pushService.requestPermissionAndGetToken).not.toHaveBeenCalled();
    });
  });

  describe('dismissNotifications', () => {
    it('should hide banner and store choice', () => {
      component.showBanner = true;
      component.dismissNotifications();
      expect(component.showBanner).toBeFalse();
      expect(localStorage.getItem('notification_choice')).toBe('dismissed');
    });
  });
});