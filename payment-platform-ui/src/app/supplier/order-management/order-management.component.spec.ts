import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { of, throwError, Subscription } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { OrderManagementComponent } from './order-management.component';
import { OrderService } from '../../services/order.service';
import { OrganizationService } from '../../services/organization.service';
import { ToastService } from '../../services/toast.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('OrderManagementComponent', () => {
  let component: OrderManagementComponent;
  let fixture: ComponentFixture<OrderManagementComponent>;
  let orderService: jasmine.SpyObj<OrderService>;
  let orgService: jasmine.SpyObj<OrganizationService>;
  let toast: jasmine.SpyObj<ToastService>;

  const mockOrder = {
    id: 1, reference: 'ORD-001', supplierId: 1, shopId: 2, createdBy: 3, createdByRole: 'SHOP_ADMIN',
    source: 'MANUAL', status: 'DRAFT', subtotal: 100, taxRate: 0.19, taxAmount: 19, total: 119,
    currency: 'TND', deliveryAgentId: null, receivedBy: null, receivedAt: null, deliveredAt: null,
    asapPayment: false, notes: '', version: 1, createdAt: '', updatedAt: '', items: [], events: []
  };

  beforeEach(() => {
    const orderSpy = jasmine.createSpyObj('OrderService', ['list', 'getById', 'confirm', 'prepare', 'readyForDelivery', 'assignDelivery', 'deliveryReject', 'cancel']);
    const orgSpy = jasmine.createSpyObj('OrganizationService', ['listUsers']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    orderSpy.list.and.returnValue(of([]));
    orgSpy.listUsers.and.returnValue(of([]));

    TestBed.configureTestingModule({
    declarations: [OrderManagementComponent],
    schemas: [NO_ERRORS_SCHEMA],
    imports: [FormsModule],
    providers: [
        { provide: OrderService, useValue: orderSpy },
        { provide: OrganizationService, useValue: orgSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: Router, useValue: routerSpy },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(OrderManagementComponent);
    component = fixture.componentInstance;
    orderService = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
    orgService = TestBed.inject(OrganizationService) as jasmine.SpyObj<OrganizationService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('ngOnInit', () => {
    it('should load orders and agents', () => {
      orderService.list.and.returnValue(of([mockOrder]));
      orgService.listUsers.and.returnValue(of([
        { id: 1, firstName: 'A', lastName: 'B', roles: ['SUPPLIER_AGENT'] },
        { id: 2, firstName: 'C', lastName: 'D', roles: ['DELIVERY_AGENT'] },
        { id: 3, firstName: 'E', lastName: 'F', roles: ['SHOP_ADMIN'] }
      ]));
      component.ngOnInit();
      expect(component.orders.length).toBe(1);
      expect(component.agents.length).toBe(2);
    });

    it('should handle load orders error', () => {
      orderService.list.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.ngOnInit();
      expect(toast.error).toHaveBeenCalledWith('Err');
      expect(component.loading).toBeFalse();
    });

    it('should handle load agents error', () => {
      orgService.listUsers.and.returnValue(throwError(() => new Error('fail')));
      component.ngOnInit();
      expect(component.loading).toBeFalse();
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
    it('should return French labels for all statuses', () => {
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
    it('should return correct CSS classes for all statuses', () => {
      expect(component.statusClass('DRAFT')).toBe('draft');
      expect(component.statusClass('CONFIRMED')).toBe('confirmed');
      expect(component.statusClass('PREPARING')).toBe('preparing');
      expect(component.statusClass('READY_FOR_DELIVERY')).toBe('ready');
      expect(component.statusClass('IN_DELIVERY')).toBe('in-delivery');
      expect(component.statusClass('DELIVERED')).toBe('delivered');
      expect(component.statusClass('ACCEPTED')).toBe('accepted');
      expect(component.statusClass('CANCELLED')).toBe('cancelled');
      expect(component.statusClass('REJECTED')).toBe('rejected');
      expect(component.statusClass('DELIVERY_REJECTED')).toBe('delivery-rejected');
      expect(component.statusClass('UNKNOWN')).toBe('');
    });
  });

  describe('viewDetail', () => {
    it('should load order detail', () => {
      orderService.getById.and.returnValue(of(mockOrder));
      component.viewDetail(mockOrder);
      expect(component.selectedOrder).toBe(mockOrder);
      expect(component.showDetail).toBeTrue();
      expect(component.loadingDetail).toBeFalse();
    });

    it('should fallback to passed order on error', () => {
      orderService.getById.and.returnValue(throwError(() => new Error('fail')));
      component.viewDetail(mockOrder);
      expect(component.selectedOrder).toBe(mockOrder);
      expect(component.loadingDetail).toBeFalse();
    });
  });

  describe('closeDetail', () => {
    it('should close detail', () => {
      component.closeDetail();
      expect(component.showDetail).toBeFalse();
      expect(component.selectedOrder).toBeNull();
    });
  });

  describe('confirm', () => {
    it('should confirm order', () => {
      orderService.confirm.and.returnValue(of(mockOrder));
      component.confirm(mockOrder);
      expect(toast.success).toHaveBeenCalledWith('Commande confirmée');
    });

    it('should handle confirm error with message', () => {
      orderService.confirm.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.confirm(mockOrder);
      expect(toast.error).toHaveBeenCalledWith('Err');
    });

    it('should handle confirm error without message', () => {
      orderService.confirm.and.returnValue(throwError(() => ({})));
      component.confirm(mockOrder);
      expect(toast.error).toHaveBeenCalledWith('Erreur');
    });
  });

  describe('prepare', () => {
    it('should prepare order', () => {
      orderService.prepare.and.returnValue(of(mockOrder));
      component.prepare(mockOrder);
      expect(toast.success).toHaveBeenCalledWith('Commande mise en préparation');
    });

    it('should handle prepare error', () => {
      orderService.prepare.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.prepare(mockOrder);
      expect(toast.error).toHaveBeenCalledWith('Err');
    });
  });

  describe('ready', () => {
    it('should mark ready', () => {
      orderService.readyForDelivery.and.returnValue(of(mockOrder));
      component.ready(mockOrder);
      expect(toast.success).toHaveBeenCalledWith('Commande prête pour livraison');
    });

    it('should handle ready error', () => {
      orderService.readyForDelivery.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.ready(mockOrder);
      expect(toast.error).toHaveBeenCalledWith('Err');
    });
  });

  describe('openAssign / closeAssign', () => {
    it('should toggle assign modal', () => {
      component.openAssign(mockOrder);
      expect(component.showAssignModal).toBeTrue();
      expect(component.assignOrderId).toBe(1);
      expect(component.assignAgentId).toBe(0);
      component.closeAssign();
      expect(component.showAssignModal).toBeFalse();
    });
  });

  describe('submitAssign', () => {
    it('should assign delivery agent', () => {
      orderService.assignDelivery.and.returnValue(of(mockOrder));
      component.assignOrderId = 1;
      component.assignAgentId = 5;
      component.submitAssign();
      expect(toast.success).toHaveBeenCalled();
      expect(component.showAssignModal).toBeFalse();
    });

    it('should not assign when no agent selected', () => {
      component.assignAgentId = '';
      component.submitAssign();
      expect(orderService.assignDelivery).not.toHaveBeenCalled();
    });

    it('should handle assign error', () => {
      orderService.assignDelivery.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.assignOrderId = 1;
      component.assignAgentId = 5;
      component.submitAssign();
      expect(toast.error).toHaveBeenCalledWith('Err');
      expect(component.assigning).toBeFalse();
    });
  });

  describe('cancel', () => {
    it('should cancel order on confirm', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      orderService.cancel.and.returnValue(of(mockOrder));
      component.cancel(mockOrder);
      expect(toast.success).toHaveBeenCalledWith('Commande annulée');
    });

    it('should not cancel when not confirmed', () => {
      spyOn(window, 'confirm').and.returnValue(false);
      component.cancel(mockOrder);
      expect(orderService.cancel).not.toHaveBeenCalled();
    });

    it('should handle cancel error', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      orderService.cancel.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.cancel(mockOrder);
      expect(toast.error).toHaveBeenCalledWith('Err');
    });
  });

  describe('deliveryReject', () => {
    it('should reject delivery on confirm', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      orderService.deliveryReject.and.returnValue(of(mockOrder));
      component.deliveryReject(mockOrder);
      expect(toast.success).toHaveBeenCalledWith('Livraison rejetée');
    });

    it('should not reject when not confirmed', () => {
      spyOn(window, 'confirm').and.returnValue(false);
      component.deliveryReject(mockOrder);
      expect(orderService.deliveryReject).not.toHaveBeenCalled();
    });

    it('should handle deliveryReject error', () => {
      spyOn(window, 'confirm').and.returnValue(true);
      orderService.deliveryReject.and.returnValue(throwError(() => ({ error: { message: 'Err' } })));
      component.deliveryReject(mockOrder);
      expect(toast.error).toHaveBeenCalledWith('Err');
    });
  });

  describe('canConfirm', () => {
    it('should return true for DRAFT', () => expect(component.canConfirm({ status: 'DRAFT' } as any)).toBeTrue());
    it('should return false for CONFIRMED', () => expect(component.canConfirm({ status: 'CONFIRMED' } as any)).toBeFalse());
  });

  describe('canPrepare', () => {
    it('should return true for CONFIRMED', () => expect(component.canPrepare({ status: 'CONFIRMED' } as any)).toBeTrue());
    it('should return false for DRAFT', () => expect(component.canPrepare({ status: 'DRAFT' } as any)).toBeFalse());
  });

  describe('canReady', () => {
    it('should return true for PREPARING', () => expect(component.canReady({ status: 'PREPARING' } as any)).toBeTrue());
    it('should return false for CONFIRMED', () => expect(component.canReady({ status: 'CONFIRMED' } as any)).toBeFalse());
  });

  describe('canAssign', () => {
    it('should return true for READY_FOR_DELIVERY', () => expect(component.canAssign({ status: 'READY_FOR_DELIVERY' } as any)).toBeTrue());
    it('should return false for IN_DELIVERY', () => expect(component.canAssign({ status: 'IN_DELIVERY' } as any)).toBeFalse());
  });

  describe('canDeliveryReject', () => {
    it('should return true for IN_DELIVERY', () => expect(component.canDeliveryReject({ status: 'IN_DELIVERY' } as any)).toBeTrue());
    it('should return false for DELIVERED', () => expect(component.canDeliveryReject({ status: 'DELIVERED' } as any)).toBeFalse());
  });

  describe('canCancel', () => {
    it('should return true for DRAFT', () => expect(component.canCancel({ status: 'DRAFT' } as any)).toBeTrue());
    it('should return true for CONFIRMED', () => expect(component.canCancel({ status: 'CONFIRMED' } as any)).toBeTrue());
    it('should return true for PREPARING', () => expect(component.canCancel({ status: 'PREPARING' } as any)).toBeTrue());
    it('should return true for READY_FOR_DELIVERY', () => expect(component.canCancel({ status: 'READY_FOR_DELIVERY' } as any)).toBeTrue());
    it('should return true for IN_DELIVERY', () => expect(component.canCancel({ status: 'IN_DELIVERY' } as any)).toBeTrue());
    it('should return false for DELIVERED', () => expect(component.canCancel({ status: 'DELIVERED' } as any)).toBeFalse());
    it('should return false for CANCELLED', () => expect(component.canCancel({ status: 'CANCELLED' } as any)).toBeFalse());
    it('should return false for ACCEPTED', () => expect(component.canCancel({ status: 'ACCEPTED' } as any)).toBeFalse());
    it('should return false for REJECTED', () => expect(component.canCancel({ status: 'REJECTED' } as any)).toBeFalse());
  });
});
