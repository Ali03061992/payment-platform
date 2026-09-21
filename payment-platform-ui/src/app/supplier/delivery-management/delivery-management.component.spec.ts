// @ts-nocheck
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { of, throwError, Subject } from 'rxjs';
import { DeliveryManagementComponent } from './delivery-management.component';
import { OrderService } from '../../services/order.service';
import { ToastService } from '../../services/toast.service';
import { Order } from '../../models/order.model';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';

describe('DeliveryManagementComponent', () => {
  let component: DeliveryManagementComponent;
  let fixture: ComponentFixture<DeliveryManagementComponent>;
  let orderService: jasmine.SpyObj<OrderService>;
  let toast: jasmine.SpyObj<ToastService>;

  const mockOrder: Order = {
    id: '1', reference: 'ORD-001', supplierId: '1', shopId: '2', supplierName: null, shopName: null,
    createdBy: '3', createdByRole: 'SUPPLIER_AGENT',
    source: 'MANUAL', status: 'IN_DELIVERY', subtotal: 100, taxRate: 0.19, taxAmount: 19, total: 119,
    currency: 'TND', deliveryAgentId: '5', deliveryAgentName: null, receivedBy: null, receivedByName: null,
    receivedAt: null, deliveredAt: null, plannedDeliveryDate: null, confirmedDeliveryDate: null,
    asapPayment: false, deliveryRejectionReason: null, notes: '', version: 1, createdAt: '', updatedAt: '', items: [], events: []
  };

  beforeEach(() => {
    const orderSpy = jasmine.createSpyObj('OrderService', ['myDeliveries', 'deliver', 'confirmDelivery', 'acceptDelivery', 'getShopAgents']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    orderSpy.myDeliveries.and.returnValue(of([]));
    orderSpy.getShopAgents.and.returnValue(of([]));

    TestBed.configureTestingModule({
    declarations: [DeliveryManagementComponent],
    imports: [FormsModule],
    providers: [
        { provide: OrderService, useValue: orderSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: ActivatedRoute, useValue: { queryParams: of({}) } },
        provideHttpClient(withInterceptorsFromDi()),
        provideHttpClientTesting()
    ]
});
    fixture = TestBed.createComponent(DeliveryManagementComponent);
    component = fixture.componentInstance;
    orderService = TestBed.inject(OrderService) as jasmine.SpyObj<OrderService>;
    toast = TestBed.inject(ToastService) as jasmine.SpyObj<ToastService>;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  describe('activeDeliveries', () => {
    it('should filter IN_DELIVERY and DELIVERY_ACCEPTED orders', () => {
      component.deliveries = [mockOrder, { ...mockOrder, id: '2', status: 'DELIVERED' }, { ...mockOrder, id: '3', status: 'DELIVERY_ACCEPTED' }];
      expect(component.activeDeliveries.length).toBe(2);
    });
  });

  describe('completedDeliveries', () => {
    it('should filter DELIVERED and ACCEPTED orders', () => {
      component.deliveries = [mockOrder, { ...mockOrder, id: '2', status: 'DELIVERED' }, { ...mockOrder, id: '3', status: 'ACCEPTED' }];
      expect(component.completedDeliveries.length).toBe(2);
    });
  });

  describe('pendingAcceptance', () => {
    it('should filter READY_FOR_DELIVERY orders', () => {
      component.deliveries = [mockOrder, { ...mockOrder, id: '2', status: 'READY_FOR_DELIVERY' }];
      expect(component.pendingAcceptance.length).toBe(1);
    });
  });

  describe('rejectedDeliveries', () => {
    it('should filter DELIVERY_REJECTED orders', () => {
      component.deliveries = [mockOrder, { ...mockOrder, id: '2', status: 'DELIVERY_REJECTED' }];
      expect(component.rejectedDeliveries.length).toBe(1);
    });
  });

  describe('acceptDelivery', () => {
    it('should accept delivery', () => {
      orderService.acceptDelivery.and.returnValue(of({ ...mockOrder, status: 'DELIVERY_ACCEPTED' }));
      component.acceptDelivery(mockOrder);
      expect(toast.success).toHaveBeenCalled();
    });
  });

  describe('openDeliver / closeDeliver', () => {
    it('should toggle deliver modal', () => {
      component.openDeliver(mockOrder);
      expect(component.showDeliverModal).toBeTrue();
      expect(component.selectedOrder).toBe(mockOrder);
      component.closeDeliver();
      expect(component.showDeliverModal).toBeFalse();
    });
  });

  describe('confirmDeliver', () => {
    it('should deliver order', () => {
      orderService.deliver.and.returnValue(of({ ...mockOrder, status: 'DELIVERED' }));
      component.selectedOrder = mockOrder;
      component.receivedBy = '3';
      component.confirmDeliver();
      expect(toast.success).toHaveBeenCalled();
      expect(component.showDeliverModal).toBeFalse();
    });

    it('should not deliver without order', () => {
      component.selectedOrder = null;
      component.receivedBy = '3';
      component.confirmDeliver();
      expect(orderService.deliver).not.toHaveBeenCalled();
    });

    it('should not deliver without receivedBy', () => {
      component.selectedOrder = mockOrder;
      component.receivedBy = '';
      component.confirmDeliver();
      expect(orderService.deliver).not.toHaveBeenCalled();
    });

    it('should handle error', () => {
      orderService.deliver.and.returnValue(throwError(() => ({ error: { message: 'Fail' } })));
      component.selectedOrder = mockOrder;
      component.receivedBy = '3';
      component.confirmDeliver();
      expect(toast.error).toHaveBeenCalled();
    });
  });

  describe('statusLabel', () => {
    it('should return correct labels', () => {
      expect(component.statusLabel('IN_DELIVERY')).toBe('En livraison');
      expect(component.statusLabel('DELIVERED')).toBe('Livré');
      expect(component.statusLabel('READY_FOR_DELIVERY')).toContain('acceptation');
      expect(component.statusLabel('DELIVERY_ACCEPTED')).toContain('Acceptée');
      expect(component.statusLabel('DELIVERY_REJECTED')).toBe('Rejetée');
      expect(component.statusLabel('UNKNOWN')).toBe('UNKNOWN');
    });
  });

  describe('statusClass', () => {
    it('should return correct classes', () => {
      expect(component.statusClass('IN_DELIVERY')).toBe('in-delivery');
      expect(component.statusClass('DELIVERED')).toBe('delivered');
      expect(component.statusClass('READY_FOR_DELIVERY')).toBe('pending');
      expect(component.statusClass('DELIVERY_ACCEPTED')).toBe('confirmed');
      expect(component.statusClass('UNKNOWN')).toBe('');
    });
  });
});
