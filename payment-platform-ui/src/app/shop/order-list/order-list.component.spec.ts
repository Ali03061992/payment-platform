import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HttpClientTestingModule } from '@angular/common/http/testing';
import { FormsModule } from '@angular/forms';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { OrderListComponent } from './order-list.component';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';

describe('OrderListComponent', () => {
  let component: OrderListComponent;
  let fixture: ComponentFixture<OrderListComponent>;
  let orderService: jasmine.SpyObj<OrderService>;
  let toast: jasmine.SpyObj<ToastService>;

  const mockOrder = {
    id: 1, reference: 'ORD-001', supplierId: 1, shopId: 2, createdBy: 3, createdByRole: 'SHOP_ADMIN',
    source: 'MANUAL', status: 'DRAFT', subtotal: 100, taxRate: 0.19, taxAmount: 19, total: 119,
    currency: 'TND', deliveryAgentId: null, receivedBy: null, receivedAt: null, deliveredAt: null,
    asapPayment: false, notes: '', version: 1, createdAt: '', updatedAt: '', items: [], events: []
  };

  beforeEach(() => {
    const orderSpy = jasmine.createSpyObj('OrderService', ['list', 'accept', 'reject', 'cancel']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    orderSpy.list.and.returnValue(of([]));

    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule, FormsModule],
      declarations: [OrderListComponent],
      schemas: [NO_ERRORS_SCHEMA],
      providers: [
        { provide: OrderService, useValue: orderSpy },
        { provide: ToastService, useValue: toastSpy }
      ]
    });
    fixture = TestBed.createComponent(OrderListComponent);
    component = fixture.componentInstance;
    orderService = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load orders', () => {
      orderService.list.and.returnValue(of([mockOrder]));
      component.ngOnInit();
      expect(component.orders.length).toBe(1);
      expect(component.loading).toBeFalse();
    });

    it('should handle load error', () => {
      orderService.list.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.ngOnInit();
      expect(toast.error).toHaveBeenCalledWith('Err');
      expect(component.loading).toBeFalse();
    });

    it('should handle load error without message', () => {
      orderService.list.and.returnValue(throwError(() => ({})));
      component.ngOnInit();
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });

  describe('ngOnDestroy', () => {
    it('should unsubscribe', () => {
      component.ngOnInit();
      expect(() => component.ngOnDestroy()).not.toThrow();
    });
  });

  describe('filteredOrders', () => {
    it('should return all when no filter', () => {
      component.orders = [mockOrder];
      component.filterStatus = '';
      expect(component.filteredOrders.length).toBe(1);
    });

    it('should filter by status', () => {
      component.orders = [mockOrder, { ...mockOrder, id: 2, status: 'CONFIRMED' }];
      component.filterStatus = 'DRAFT';
      expect(component.filteredOrders.length).toBe(1);
    });
  });

  describe('statusLabel', () => {
    it('should return correct labels for all statuses', () => {
      expect(component.statusLabel('DRAFT')).toBe('Brouillon');
      expect(component.statusLabel('CONFIRMED')).toBe('Confirmé');
      expect(component.statusLabel('PREPARING')).toBe('En préparation');
      expect(component.statusLabel('READY_FOR_DELIVERY')).toBe('Prêt pour livraison');
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
    it('should return correct classes for all statuses', () => {
      expect(component.statusClass('DRAFT')).toBe('draft');
      expect(component.statusClass('CONFIRMED')).toBe('confirmed');
      expect(component.statusClass('PREPARING')).toBe('preparing');
      expect(component.statusClass('READY_FOR_DELIVERY')).toBe('ready');
      expect(component.statusClass('IN_DELIVERY')).toBe('delivery');
      expect(component.statusClass('DELIVERED')).toBe('delivered');
      expect(component.statusClass('ACCEPTED')).toBe('accepted');
      expect(component.statusClass('CANCELLED')).toBe('cancelled');
      expect(component.statusClass('REJECTED')).toBe('rejected');
      expect(component.statusClass('DELIVERY_REJECTED')).toBe('delivery-rejected');
      expect(component.statusClass('UNKNOWN')).toBe('');
    });
  });

  describe('accept', () => {
    it('should accept order', () => {
      orderService.accept.and.returnValue(of(mockOrder));
      component.accept(1);
      expect(orderService.accept).toHaveBeenCalledWith(1);
    });

    it('should handle accept error with message', () => {
      orderService.accept.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.accept(1);
      expect(toast.error).toHaveBeenCalledWith('Fail');
    });

    it('should handle accept error without message', () => {
      orderService.accept.and.returnValue(throwError(() => ({})));
      component.accept(1);
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });

  describe('reject', () => {
    it('should reject order on confirm', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      orderService.reject.and.returnValue(of(mockOrder));
      component.reject(1);
      expect(orderService.reject).toHaveBeenCalledWith(1);
    });

    it('should not reject when not confirmed', () => {
      spyOn(window, 'confirm').and.returnValue(false);
      component.reject(1);
      expect(orderService.reject).not.toHaveBeenCalled();
    });

    it('should handle reject error with message', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      orderService.reject.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.reject(1);
      expect(toast.error).toHaveBeenCalledWith('Fail');
    });

    it('should handle reject error without message', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      orderService.reject.and.returnValue(throwError(() => ({})));
      component.reject(1);
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });

  describe('cancel', () => {
    it('should cancel order', () => {
      orderService.cancel.and.returnValue(of(mockOrder));
      component.cancel(1);
      expect(orderService.cancel).toHaveBeenCalledWith(1);
    });

    it('should handle cancel error with message', () => {
      orderService.cancel.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.cancel(1);
      expect(toast.error).toHaveBeenCalledWith('Fail');
    });

    it('should handle cancel error without message', () => {
      orderService.cancel.and.returnValue(throwError(() => ({})));
      component.cancel(1);
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });
});
