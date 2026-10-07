// @ts-nocheck
/**
 * Tests du routage des notifications (non-régression 403 boutique).
 * Perimetre : navigateNotification + isShopUser.
 * Moyens : TestBed, stubs jasmine (pas d'appel réseau).
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { of } from 'rxjs';
import { NotificationsComponent } from './notifications.component';
import { NotificationService } from '../../services/notification.service';
import { LoginService } from '../../services/login.service';
import { TranslateStubPipe, translateServiceProvider } from '../../testing/translate-stubs';

describe('NotificationsComponent', () => {
  let component: NotificationsComponent;
  let fixture: ComponentFixture<NotificationsComponent>;
  let loginService: jasmine.SpyObj<LoginService>;
  let router: jasmine.SpyObj<Router>;

  function setupRoles(roles: string[]) {
    loginService.getCurrentUser.and.returnValue({ id: 'u1', roles } as any);
    fixture = TestBed.createComponent(NotificationsComponent);
    component = fixture.componentInstance;
  }

  beforeEach(() => {
    const notifSpy = jasmine.createSpyObj('NotificationService', ['fetchNotificationsPaged', 'markAsRead', 'markAllAsRead']);
    notifSpy.fetchNotificationsPaged.and.returnValue(of({ items: [], totalElements: 0, totalPages: 0, currentPage: 0 }));
    const loginSpy = jasmine.createSpyObj('LoginService', ['getCurrentUser']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);

    TestBed.configureTestingModule({
      declarations: [NotificationsComponent, TranslateStubPipe],
      schemas: [NO_ERRORS_SCHEMA],
      imports: [],
      providers: [
        translateServiceProvider(),
        { provide: NotificationService, useValue: notifSpy },
        { provide: LoginService, useValue: loginSpy },
        { provide: Router, useValue: routerSpy }
      ]
    });
    loginService = TestBed.inject(LoginService) as jasmine.SpyObj<LoginService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should create', () => {
    setupRoles(['SHOP_ADMIN']);
    expect(component).toBeTruthy();
  });

  describe('isShopUser', () => {
    it('should return true for SHOP_ADMIN', () => {
      setupRoles(['SHOP_ADMIN']);
      expect(component.isShopUser()).toBeTrue();
    });

    it('should return true for SHOP_AGENT', () => {
      setupRoles(['SHOP_AGENT']);
      expect(component.isShopUser()).toBeTrue();
    });

    it('should return false for SUPPLIER_ADMIN', () => {
      setupRoles(['SUPPLIER_ADMIN']);
      expect(component.isShopUser()).toBeFalse();
    });
  });

  describe('navigateNotification', () => {
    it('should route ORDER notification to shop order detail for SHOP_ADMIN (no 403)', () => {
      setupRoles(['SHOP_ADMIN']);
      component.navigateNotification({ relatedEntityType: 'ORDER', relatedEntityId: 'ORD-1' } as any);
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/shop/orders', 'ORD-1']);
    });

    it('should route ORDER notification to supplier list for SUPPLIER_ADMIN', () => {
      setupRoles(['SUPPLIER_ADMIN']);
      component.navigateNotification({ relatedEntityType: 'ORDER', relatedEntityId: 'ORD-1' } as any);
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/supplier/orders'], { queryParams: { ref: 'ORD-1' } });
    });

    it('should route DELIVERY notification to shop deliveries for SHOP_ADMIN (no 403)', () => {
      setupRoles(['SHOP_ADMIN']);
      component.navigateNotification({ relatedEntityType: 'DELIVERY', relatedEntityId: 'order-1' } as any);
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/shop/deliveries'], { queryParams: { orderId: 'order-1' } });
    });

    it('should route DELIVERY notification to supplier deliveries for SUPPLIER_ADMIN', () => {
      setupRoles(['SUPPLIER_ADMIN']);
      component.navigateNotification({ relatedEntityType: 'DELIVERY', relatedEntityId: 'order-1' } as any);
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/supplier/deliveries'], { queryParams: { orderId: 'order-1' } });
    });

    it('should route PAYMENT notification to payment detail', () => {
      setupRoles(['SHOP_ADMIN']);
      component.navigateNotification({ relatedEntityType: 'PAYMENT', relatedEntityId: 'pay-1' } as any);
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/payments', 'pay-1']);
    });

    it('should route DISPUTE notification to shop disputes for SHOP_ADMIN', () => {
      setupRoles(['SHOP_ADMIN']);
      component.navigateNotification({ relatedEntityType: 'DISPUTE', relatedEntityId: 'd-1' } as any);
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/shop/disputes', 'd-1']);
    });

    it('should do nothing without related entity', () => {
      setupRoles(['SHOP_ADMIN']);
      component.navigateNotification({ relatedEntityType: 'ORDER' } as any);
      expect(router.navigate).not.toHaveBeenCalled();
    });
  });
});
