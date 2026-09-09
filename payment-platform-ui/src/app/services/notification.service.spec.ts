import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { NotificationService } from './notification.service';
import { Notification } from '../models/notification.model';

describe('NotificationService', () => {
  let service: NotificationService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [NotificationService]
    });
    service = TestBed.inject(NotificationService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    service.stopPolling();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('fetchNotifications', () => {
    it('should GET notifications and update subject', () => {
      const mock: Notification[] = [
        { id: 1, recipientUserId: 1, recipientOrganizationId: null, type: 'PAYMENT_CREATED',
          message: 'New payment', readStatus: 'UNREAD', createdAt: '', readAt: null,
          relatedEntityType: 'PAYMENT', relatedEntityId: '1' }
      ];
      service.fetchNotifications().subscribe(data => {
        expect(data.length).toBe(1);
      });
      const req = httpMock.expectOne('/api/notifications');
      expect(req.request.method).toBe('GET');
      req.flush(mock);

      let latest: Notification[] = [];
      service.notifications$.subscribe(n => latest = n);
      expect(latest.length).toBe(1);
    });

    it('should handle null response', () => {
      service.fetchNotifications().subscribe();
      const req = httpMock.expectOne('/api/notifications');
      req.flush(null);
    });
  });

  describe('fetchUnreadCount', () => {
    it('should GET unread count and update subject', () => {
      service.fetchUnreadCount().subscribe();
      const req = httpMock.expectOne('/api/notifications/unread-count');
      expect(req.request.method).toBe('GET');
      req.flush({ count: 5 });

      let count = 0;
      service.unreadCount$.subscribe(c => count = c);
      expect(count).toBe(5);
    });

    it('should handle count 0', () => {
      service.fetchUnreadCount().subscribe();
      const req = httpMock.expectOne('/api/notifications/unread-count');
      req.flush({ count: 0 });
    });
  });

  describe('markAsRead', () => {
    it('should POST to mark notification as read', () => {
      service.markAsRead(1).subscribe();
      const req = httpMock.expectOne('/api/notifications/1/read');
      expect(req.request.method).toBe('POST');
      req.flush({});
    });
  });

  describe('markAllAsRead', () => {
    it('should POST to mark all as read', () => {
      service.markAllAsRead().subscribe(res => {
        expect(res.updated).toBe(3);
      });
      const req = httpMock.expectOne('/api/notifications/read-all');
      expect(req.request.method).toBe('POST');
      req.flush({ updated: 3 });
    });
  });

  describe('startPolling / stopPolling', () => {
    it('should start and stop polling', () => {
      service.startPolling(1000);
      const req1 = httpMock.expectOne('/api/notifications');
      req1.flush([]);
      const req2 = httpMock.expectOne('/api/notifications/unread-count');
      req2.flush({ count: 0 });

      service.stopPolling();
    });

    it('should call stopPolling before starting again', () => {
      service.startPolling(1000);
      const req1 = httpMock.expectOne('/api/notifications');
      req1.flush([]);
      const req2 = httpMock.expectOne('/api/notifications/unread-count');
      req2.flush({ count: 0 });

      service.startPolling(2000);
      const req3 = httpMock.expectOne('/api/notifications');
      req3.flush([]);
      const req4 = httpMock.expectOne('/api/notifications/unread-count');
      req4.flush({ count: 0 });

      service.stopPolling();
    });
  });

  describe('requestPermission', () => {
    it('should return a promise', () => {
      const result = service.requestPermission();
      expect(result).toBeTruthy();
      expect(typeof result.then).toBe('function');
    });
  });

  describe('getPermissionStatus', () => {
    it('should return a permission status', () => {
      const result = service.getPermissionStatus();
      expect(result).toBeTruthy();
    });
  });

  describe('showBrowserNotification', () => {
    it('should be callable without error', () => {
      expect(() => service.showBrowserNotification()).not.toThrow();
    });
  });

  describe('fetchUnreadCount - count increase detection', () => {
    it('should show browser notification when count increases from non-zero', () => {
      spyOn(service, 'showBrowserNotification');
      service['lastUnreadCount'] = 2;
      service.fetchUnreadCount().subscribe();
      const req = httpMock.expectOne('/api/notifications/unread-count');
      req.flush({ count: 5 });
      expect(service.showBrowserNotification).toHaveBeenCalled();
    });

    it('should not show browser notification when count decreases', () => {
      spyOn(service, 'showBrowserNotification');
      service['lastUnreadCount'] = 5;
      service.fetchUnreadCount().subscribe();
      const req = httpMock.expectOne('/api/notifications/unread-count');
      req.flush({ count: 2 });
      expect(service.showBrowserNotification).not.toHaveBeenCalled();
    });

    it('should not show browser notification when count stays the same', () => {
      spyOn(service, 'showBrowserNotification');
      service['lastUnreadCount'] = 3;
      service.fetchUnreadCount().subscribe();
      const req = httpMock.expectOne('/api/notifications/unread-count');
      req.flush({ count: 3 });
      expect(service.showBrowserNotification).not.toHaveBeenCalled();
    });

    it('should not show browser notification when previous count was 0', () => {
      spyOn(service, 'showBrowserNotification');
      service['lastUnreadCount'] = 0;
      service.fetchUnreadCount().subscribe();
      const req = httpMock.expectOne('/api/notifications/unread-count');
      req.flush({ count: 5 });
      expect(service.showBrowserNotification).not.toHaveBeenCalled();
    });
  });
});
