// @ts-nocheck
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ShopOrderDetailComponent } from './order-detail.component';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('ShopOrderDetailComponent', () => {
  let component: ShopOrderDetailComponent;
  let fixture: ComponentFixture<ShopOrderDetailComponent>;
  let orderService: jasmine.SpyObj<OrderService>;
  let toast: jasmine.SpyObj<ToastService>;
  let router: jasmine.SpyObj<Router>;

  const mockOrder = {
    id: 1, reference: 'ORD-001', supplierId: 1, shopId: 2, createdBy: 3, createdByRole: 'SHOP_ADMIN',
    source: 'MANUAL', status: 'DRAFT', subtotal: 100, taxRate: 0.19, taxAmount: 19, total: 119,
    currency: 'TND', deliveryAgentId: null, deliveryAgentName: null, receivedBy: null, receivedByName: null,
    receivedAt: null, deliveredAt: null, plannedDeliveryDate: null, confirmedDeliveryDate: null,
    asapPayment: false, deliveryRejectionReason: null, notes: '', version: 1, createdAt: '', updatedAt: '', items: [], events: []
  };

  beforeEach(() => {
    const orderSpy = jasmine.createSpyObj('OrderService', ['getById', 'accept', 'acceptAsap', 'reject', 'cancel']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    orderSpy.getById.and.returnValue(of(mockOrder));

    TestBed.configureTestingModule({
    declarations: [ShopOrderDetailComponent],
    schemas: [NO_ERRORS_SCHEMA],
    imports: [],
    providers: [
        { provide: OrderService, useValue: orderSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: Router, useValue: routerSpy },
        { provide: ActivatedRoute, useValue: { snapshot: { paramMap: convertToParamMap({ id: '1' }) } } },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(ShopOrderDetailComponent);
    component = fixture.componentInstance;
    orderService = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
    router = TestBed.inject(Router) as jasmine.SpyObj<Router>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load order', () => {
      component.ngOnInit();
      expect(component.order).toBeTruthy();
      expect(component.loading).toBeFalse();
    });

    it('should navigate on load error', () => {
      orderService.getById.and.returnValue(throwError(() => new Error('fail')));
      component.ngOnInit();
      expect(component.loading).toBeFalse();
      expect(router.navigate).toHaveBeenCalledWith(['/dashboard/shop/orders']);
    });
  });

  describe('ngOnDestroy', () => {
    it('should unsubscribe', () => {
      component.ngOnInit();
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });

  describe('accept', () => {
    it('should accept order', () => {
      orderService.accept.and.returnValue(of({ ...mockOrder, status: 'ACCEPTED' }));
      component.order = mockOrder;
      component.accept();
      expect(toast.success).toHaveBeenCalledWith('Commande acceptée');
    });

    it('should not accept when no order', () => {
      component.order = null;
      component.accept();
      expect(orderService.accept).not.toHaveBeenCalled();
    });

    it('should handle accept error with message', () => {
      orderService.accept.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.order = mockOrder;
      component.accept();
      expect(toast.error).toHaveBeenCalledWith('Fail');
    });

    it('should handle accept error without message', () => {
      orderService.accept.and.returnValue(throwError(() => ({})));
      component.order = mockOrder;
      component.accept();
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });

  describe('acceptAsap', () => {
    it('should accept ASAP', () => {
      orderService.acceptAsap.and.returnValue(of({ ...mockOrder, status: 'ACCEPTED' }));
      component.order = mockOrder;
      component.acceptAsap();
      expect(toast.success).toHaveBeenCalledWith('Commande acceptée avec paiement ASAP');
    });

    it('should not acceptAsap when no order', () => {
      component.order = null;
      component.acceptAsap();
      expect(orderService.acceptAsap).not.toHaveBeenCalled();
    });

    it('should handle acceptAsap error', () => {
      orderService.acceptAsap.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.order = mockOrder;
      component.acceptAsap();
      expect(toast.error).toHaveBeenCalledWith('Fail');
    });
  });

  describe('reject', () => {
    it('should reject on confirm', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      orderService.reject.and.returnValue(of({ ...mockOrder, status: 'REJECTED' }));
      component.order = mockOrder;
      component.reject();
      expect(toast.success).toHaveBeenCalledWith('Commande rejetée');
    });

    it('should not reject when not confirmed', () => {
      spyOn(window, 'confirm').and.returnValue(false);
      component.order = mockOrder;
      component.reject();
      expect(orderService.reject).not.toHaveBeenCalled();
    });

    it('should not reject when no order', () => {
      component.order = null;
      component.reject();
      expect(orderService.reject).not.toHaveBeenCalled();
    });

    it('should handle reject error', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      orderService.reject.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.order = mockOrder;
      component.reject();
      expect(toast.error).toHaveBeenCalledWith('Fail');
    });
  });

  describe('cancel', () => {
    it('should cancel order', () => {
      orderService.cancel.and.returnValue(of({ ...mockOrder, status: 'CANCELLED' }));
      component.order = mockOrder;
      component.cancel();
      expect(toast.success).toHaveBeenCalledWith('Commande annulée');
    });

    it('should not cancel when no order', () => {
      component.order = null;
      component.cancel();
      expect(orderService.cancel).not.toHaveBeenCalled();
    });

    it('should handle cancel error', () => {
      orderService.cancel.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.order = mockOrder;
      component.cancel();
      expect(toast.error).toHaveBeenCalledWith('Fail');
    });
  });

  describe('statusLabel', () => {
    it('should return correct labels', () => {
      expect(component.statusLabel('DRAFT')).toBe('Brouillon');
      expect(component.statusLabel('CONFIRMED')).toBe('Confirmé');
      expect(component.statusLabel('PREPARING')).toBe('En préparation');
      expect(component.statusLabel('READY_FOR_DELIVERY')).toBe('Prêt pour livraison');
      expect(component.statusLabel('DELIVERY_ACCEPTED')).toBe('Livraison acceptée');
      expect(component.statusLabel('IN_DELIVERY')).toBe('En livraison');
      expect(component.statusLabel('DELIVERED')).toBe('Livré');
      expect(component.statusLabel('ACCEPTED')).toBe('Accepté');
      expect(component.statusLabel('CANCELLED')).toBe('Annulé');
      expect(component.statusLabel('REJECTED')).toBe('Rejeté');
      expect(component.statusLabel('DELIVERY_REJECTED')).toBe('Livraison rejetée');
      expect(component.statusLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });

  describe('statusClass', () => {
    it('should return correct classes', () => {
      expect(component.statusClass('DRAFT')).toBe('draft');
      expect(component.statusClass('CONFIRMED')).toBe('confirmed');
      expect(component.statusClass('PREPARING')).toBe('preparing');
      expect(component.statusClass('READY_FOR_DELIVERY')).toBe('ready');
      expect(component.statusClass('DELIVERY_ACCEPTED')).toBe('confirmed');
      expect(component.statusClass('IN_DELIVERY')).toBe('delivery');
      expect(component.statusClass('DELIVERED')).toBe('delivered');
      expect(component.statusClass('ACCEPTED')).toBe('accepted');
      expect(component.statusClass('CANCELLED')).toBe('cancelled');
      expect(component.statusClass('REJECTED')).toBe('rejected');
      expect(component.statusClass('DELIVERY_REJECTED')).toBe('delivery-rejected');
      expect(component.statusClass('UNKNOWN')).toBe('');
    });
  });

  describe('actionLabel', () => {
    it('should return correct labels', () => {
      expect(component.actionLabel('ORDER_CREATED')).toBe('Créé');
      expect(component.actionLabel('ORDER_CONFIRMED')).toBe('Confirmé');
      expect(component.actionLabel('ORDER_PREPARING')).toBe('En préparation');
      expect(component.actionLabel('ORDER_READY_FOR_DELIVERY')).toBe('Prêt');
      expect(component.actionLabel('ORDER_IN_DELIVERY')).toBe('En livraison');
      expect(component.actionLabel('ORDER_DELIVERED')).toBe('Livré');
      expect(component.actionLabel('ORDER_ACCEPTED')).toBe('Accepté');
      expect(component.actionLabel('ORDER_CANCELLED')).toBe('Annulé');
      expect(component.actionLabel('ORDER_ACCEPTED_ASAP')).toBe('Accepté (ASAP)');
      expect(component.actionLabel('ORDER_REJECTED')).toBe('Rejeté');
      expect(component.actionLabel('ORDER_DELIVERY_REJECTED')).toBe('Livraison rejetée');
      expect(component.actionLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });

  describe('isStepCompleted', () => {
    it('should return true for completed steps', () => {
      component.order = { ...mockOrder, status: 'PREPARING' };
      expect(component.isStepCompleted(0)).toBeTrue();
      expect(component.isStepCompleted(1)).toBeTrue();
      expect(component.isStepCompleted(2)).toBeTrue();
      expect(component.isStepCompleted(3)).toBeFalse();
    });

    it('should return false when no order', () => {
      component.order = null;
      expect(component.isStepCompleted(0)).toBeFalse();
    });
  });

  describe('isCurrentStep', () => {
    it('should return true for current step', () => {
      component.order = { ...mockOrder, status: 'CONFIRMED' };
      expect(component.isCurrentStep('CONFIRMED')).toBeTrue();
      expect(component.isCurrentStep('DRAFT')).toBeFalse();
    });
  });
});
