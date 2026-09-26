// @ts-nocheck
/**
 * Tests du composant NotificationBannerComponent.
 * Perimetre : instanciation et blocs ngOnInit, acceptNotifications, dismissNotifications (voir blocs describe/it).
 * Moyens : TestBed + fixture, stubs jasmine.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NotificationBannerComponent } from './notification-banner.component';
import { NotificationService } from '../../services/notification.service';
import { PushNotificationService } from '../../services/push-notification.service';

describe('NotificationBannerComponent', () => {
  let component: NotificationBannerComponent;
  let fixture: ComponentFixture<NotificationBannerComponent>;
  let notificationService: jasmine.SpyObj<NotificationService>;
  let pushService: jasmine.SpyObj<PushNotificationService>;

  beforeEach(() => {
    const notifSpy = jasmine.createSpyObj('NotificationService', ['getPermissionStatus', 'requestPermission', 'showBrowserNotification']);
    notifSpy.getPermissionStatus.and.returnValue('default');
    const pushSpy = jasmine.createSpyObj('PushNotificationService', ['requestPermissionAndGetToken', 'listenToMessages']);
    pushSpy.requestPermissionAndGetToken.and.returnValue(Promise.resolve('fcm-token'));
    localStorage.clear();

    TestBed.configureTestingModule({
      declarations: [NotificationBannerComponent],
      providers: [
        { provide: NotificationService, useValue: notifSpy },
        { provide: PushNotificationService, useValue: pushSpy }
      ]
    });
    fixture = TestBed.createComponent(NotificationBannerComponent);
    component = fixture.componentInstance;
    notificationService = TestBed.inject(NotificationService) as jasmine.SpyObj<NotificationService>;
    pushService = TestBed.inject(PushNotificationService) as jasmine.SpyObj<PushNotificationService>;
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

    it('should register FCM token when permission granted', async () => {
      notificationService.requestPermission.and.returnValue(Promise.resolve('granted'));
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
