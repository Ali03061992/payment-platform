import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of, Subject } from 'rxjs';
import { LayoutComponent } from './layout.component';
import { LoginService } from '../services/login.service';
import { NotificationService } from '../services/notification.service';

describe('LayoutComponent', () => {
  let component: LayoutComponent;
  let fixture: ComponentFixture<LayoutComponent>;
  let loginService: jasmine.SpyObj<LoginService>;
  let notificationService: jasmine.SpyObj<NotificationService>;
  let router: jasmine.SpyObj<Router>;
  let notificationsSubject: Subject<any[]>;
  let unreadCountSubject: Subject<number>;

  beforeEach(() => {
    notificationsSubject = new Subject();
    unreadCountSubject = new Subject();
    const loginSpy = jasmine.createSpyObj('LoginService', ['getCurrentUser', 'logout']);
    const notifSpy = jasmine.createSpyObj('NotificationService', ['startPolling', 'stopPolling', 'markAllAsRead']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    notifSpy.notifications$ = notificationsSubject.asObservable();
    notifSpy.unreadCount$ = unreadCountSubject.asObservable();
    notifSpy.markAllAsRead.and.returnValue(of({ updated: 1 } as any));
    loginSpy.getCurrentUser.and.returnValue({ id: 1, username: 'admin', firstName: 'A', lastName: 'B', roles: ['SYSTEM_ADMIN'], organizationId: 1 } as any);

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      declarations: [LayoutComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: LoginService, useValue: loginSpy },
        { provide: NotificationService, useValue: notifSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
    fixture = TestBed.createComponent(LayoutComponent);
    component = fixture.componentInstance;
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
    notificationService = TestBed.inject(NotificationService) as jasmine.SpyObj<NotificationService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should have user loaded', () => {
    expect(component.user).toBeTruthy();
    expect(component.user.username).toBe('admin');
  });

  describe('ngOnInit', () => {
    it('should start polling and subscribe to notifications', () => {
      component.ngOnInit();
      expect(notificationService.startPolling).toHaveBeenCalledWith(30000);
      notificationsSubject.next([{ id: 1 } as any]);
      unreadCountSubject.next(5);
      expect(component.notifications.length).toBe(1);
      expect(component.unreadCount).toBe(5);
    });
  });

  describe('ngOnDestroy', () => {
    it('should unsubscribe from all subscriptions', () => {
      component.ngOnInit();
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });

  describe('filteredNav', () => {
    it('should return nav items matching user roles', () => {
      const filtered = component.filteredNav;
      expect(filtered.length).toBeGreaterThan(0);
      expect(filtered.some(i => i.route === 'admin/users')).toBeTrue();
    });

    it('should not return SYSTEM_ADMIN items for SUPPLIER user', () => {
      component.user = { roles: ['SUPPLIER_ADMIN'] };
      const filtered = component.filteredNav;
      expect(filtered.some(i => i.route === 'admin/users')).toBeFalse();
    });
  });

  describe('toggleSidebar', () => {
    it('should toggle sidebar', () => {
      expect(component.sidebarOpen).toBeFalse();
      component.toggleSidebar();
      expect(component.sidebarOpen).toBeTrue();
      component.toggleSidebar();
      expect(component.sidebarOpen).toBeFalse();
    });
  });

  describe('onNavClick', () => {
    it('should close sidebar on mobile', () => {
      component.isMobile = true;
      component.sidebarOpen = true;
      component.onNavClick();
      expect(component.sidebarOpen).toBeFalse();
    });

    it('should not close sidebar on desktop', () => {
      component.isMobile = false;
      component.sidebarOpen = true;
      component.onNavClick();
      expect(component.sidebarOpen).toBeTrue();
    });
  });

  describe('onResize', () => {
    it('should update isMobile', () => {
      component.onResize();
      expect(typeof component.isMobile).toBe('boolean');
    });
  });

  describe('onTouchStart', () => {
    it('should not set start on desktop', () => {
      component.isMobile = false;
      component.onTouchStart({ touches: [{ clientX: 10, clientY: 20 }] } as any);
      expect(component['touchStartX']).toBe(0);
    });

    it('should set start on mobile', () => {
      component.isMobile = true;
      component.onTouchStart({ touches: [{ clientX: 10, clientY: 20 }] } as any);
      expect(component['touchStartX']).toBe(10);
      expect(component['touchStartY']).toBe(20);
    });
  });

  describe('onTouchMove', () => {
    it('should not process on desktop', () => {
      component.isMobile = false;
      component.onTouchMove({ touches: [{ clientX: 50, clientY: 20 }] } as any);
      expect(component.sidebarOpen).toBeFalse();
    });

    it('should open sidebar when swiping right from left edge on mobile', () => {
      component.isMobile = true;
      component['touchStartX'] = 10;
      component.sidebarOpen = false;
      const event = { touches: [{ clientX: 100, clientY: 20 }], preventDefault: jasmine.createSpy('preventDefault') } as any;
      component.onTouchMove(event);
      expect(component.sidebarOpen).toBeTrue();
      expect(event.preventDefault).toHaveBeenCalled();
    });

    it('should close sidebar when swiping left on mobile', () => {
      component.isMobile = true;
      component['touchStartX'] = 100;
      component.sidebarOpen = true;
      const event = { touches: [{ clientX: 10, clientY: 20 }], preventDefault: jasmine.createSpy('preventDefault') } as any;
      component.onTouchMove(event);
      expect(component.sidebarOpen).toBeFalse();
      expect(event.preventDefault).toHaveBeenCalled();
    });

    it('should not toggle sidebar when vertical movement is dominant', () => {
      component.isMobile = true;
      component['touchStartX'] = 10;
      component.sidebarOpen = false;
      const event = { touches: [{ clientX: 50, clientY: 100 }], preventDefault: jasmine.createSpy('preventDefault') } as any;
      component.onTouchMove(event);
      expect(component.sidebarOpen).toBeFalse();
      expect(event.preventDefault).not.toHaveBeenCalled();
    });
  });

  describe('onTouchEnd', () => {
    it('should set isSwiping to false on mobile', () => {
      component.isMobile = true;
      component['isSwiping'] = true;
      component.onTouchEnd({ touches: [] } as any);
      expect(component['isSwiping']).toBeFalse();
    });

    it('should not process on desktop', () => {
      component.isMobile = false;
      component['isSwiping'] = true;
      component.onTouchEnd({ touches: [] } as any);
      expect(component['isSwiping']).toBeTrue();
    });
  });

  describe('toggleNotifications', () => {
    it('should toggle notifications panel', () => {
      component.showNotifications = false;
      component.toggleNotifications();
      expect(component.showNotifications).toBeTrue();
    });

    it('should mark all read when opening with unread count > 0', () => {
      component.unreadCount = 5;
      component.showNotifications = false;
      component.toggleNotifications();
      expect(notificationService.markAllAsRead).toHaveBeenCalled();
      expect(component.unreadCount).toBe(0);
    });

    it('should not mark all read when opening with no unread count', () => {
      component.unreadCount = 0;
      component.showNotifications = false;
      component.toggleNotifications();
      expect(notificationService.markAllAsRead).not.toHaveBeenCalled();
    });
  });

  describe('closeNotifications', () => {
    it('should close notifications', () => {
      component.showNotifications = true;
      component.closeNotifications();
      expect(component.showNotifications).toBeFalse();
    });
  });

  describe('markAllRead', () => {
    it('should mark all read and reset unread count', () => {
      component.unreadCount = 3;
      component.notifications = [{ id: 1, readStatus: 'UNREAD' } as any, { id: 2, readStatus: 'UNREAD' } as any];
      component.markAllRead();
      expect(component.unreadCount).toBe(0);
      expect(component.notifications.every(n => n.readStatus === 'READ')).toBeTrue();
    });

    it('should not reset when no updates', () => {
      notificationService.markAllAsRead.and.returnValue(of({ updated: 0 } as any));
      component.unreadCount = 3;
      component.markAllRead();
      expect(component.unreadCount).toBe(3);
    });
  });

  describe('getNotificationIcon', () => {
    it('should return correct icons', () => {
      expect(component.getNotificationIcon('PAYMENT_CREATED')).toBe('💸');
      expect(component.getNotificationIcon('PAYMENT_CONFIRMED')).toBe('✅');
      expect(component.getNotificationIcon('PAYMENT_REJECTED')).toBe('❌');
      expect(component.getNotificationIcon('PAYMENT_CANCELLED')).toBe('🚫');
      expect(component.getNotificationIcon('UNKNOWN')).toBe('🔔');
    });
  });

  describe('navigateNotification', () => {
    it('should navigate for payment entity', () => {
      component.navigateNotification({ relatedEntityType: 'PAYMENT', relatedEntityId: 42 } as any);
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/payments'], { queryParams: { ref: 42 } });
      expect(component.showNotifications).toBeFalse();
    });

    it('should not navigate for non-payment entity', () => {
      component.navigateNotification({ relatedEntityType: 'OTHER', relatedEntityId: 1 } as any);
      expect(router.navigate).not.toHaveBeenCalled();
      expect(component.showNotifications).toBeFalse();
    });

    it('should not navigate when no entity id', () => {
      component.navigateNotification({ relatedEntityType: 'PAYMENT' } as any);
      expect(router.navigate).not.toHaveBeenCalled();
      expect(component.showNotifications).toBeFalse();
    });
  });

  describe('logout', () => {
    it('should call logout and navigate', () => {
      component.logout();
      expect(loginService.logout).toHaveBeenCalled();
      expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
  });

  describe('getInitials', () => {
    it('should return initials from user', () => {
      expect(component.getInitials()).toBe('AB');
    });

    it('should return ? when no user', () => {
      component.user = null;
      expect(component.getInitials()).toBe('?');
    });

    it('should handle user with no first/last name', () => {
      component.user = { firstName: '', lastName: '' };
      expect(component.getInitials()).toBe('');
    });

    it('should handle user with only first name', () => {
      component.user = { firstName: 'John', lastName: '' };
      expect(component.getInitials()).toBe('J');
    });
  });

  describe('getTimeAgo', () => {
    it('should return instant for recent time', () => {
      const now = new Date().toISOString();
      expect(component.getTimeAgo(now)).toContain("instant");
    });

    it('should return minutes for 5 min ago', () => {
      const fiveMinAgo = new Date(Date.now() - 5 * 60000).toISOString();
      expect(component.getTimeAgo(fiveMinAgo)).toContain('5min');
    });

    it('should return hours for 2 hours ago', () => {
      const twoHoursAgo = new Date(Date.now() - 2 * 3600000).toISOString();
      expect(component.getTimeAgo(twoHoursAgo)).toContain('2h');
    });

    it('should return days for 3 days ago', () => {
      const threeDaysAgo = new Date(Date.now() - 3 * 86400000).toISOString();
      expect(component.getTimeAgo(threeDaysAgo)).toContain('3j');
    });
  });

  describe('checkMobile (private)', () => {
    it('should set isMobile and close sidebar when innerWidth <= 768', () => {
      component.sidebarOpen = true;
      Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 500 });
      component['checkMobile']();
      expect(component.isMobile).toBeTrue();
      expect(component.sidebarOpen).toBeFalse();
    });

    it('should not close sidebar when innerWidth > 768', () => {
      Object.defineProperty(window, 'innerWidth', { writable: true, configurable: true, value: 1024 });
      component.sidebarOpen = true;
      component['checkMobile']();
      expect(component.isMobile).toBeFalse();
      expect(component.sidebarOpen).toBeTrue();
    });
  });
});
