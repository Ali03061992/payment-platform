// @ts-nocheck
/**
 * Tests du composant ShopOrderDetailComponent.
 * Perimetre : instanciation et blocs ngOnInit, ngOnDestroy, accept, acceptAsap, reject (voir blocs describe/it).
 * Moyens : TestBed + fixture, stubs jasmine, client HTTP de test, observables RxJS mockes.
 */
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, ActivatedRoute, convertToParamMap } from '@angular/router';
import { of, throwError } from 'rxjs';
import { NO_ERRORS_SCHEMA } from '@angular/core';
import { ShopOrderDetailComponent } from './order-detail.component';
import { OrderService } from '../../services/order.service';
import { DisputeService } from '../../services/dispute.service';
import { ToastService } from '../../services/toast.service';
import { ConfirmDialogService } from '../../components/confirm-dialog/confirm-dialog.service';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { TranslateStubPipe, translateServiceProvider } from '../../testing/translate-stubs';
import { StatusLabelPipe } from '../../pipes/status-label.pipe';
import { TimeAgoPipe } from '../../pipes/time-ago.pipe';

describe('ShopOrderDetailComponent', () => {
  let component: ShopOrderDetailComponent;
  let fixture: ComponentFixture<ShopOrderDetailComponent>;
  let orderService: jasmine.SpyObj<OrderService>;
  let toast: jasmine.SpyObj<ToastService>;
  let confirmDialog: jasmine.SpyObj<ConfirmDialogService>;
  let router: jasmine.SpyObj<Router>;

  const mockOrder = {
    id: 1, reference: 'ORD-001', supplierId: 1, shopId: 2, createdBy: 3, createdByName: null, createdByRole: 'SHOP_ADMIN',
    source: 'MANUAL', status: 'DRAFT', subtotal: 100, taxRate: 0.19, taxAmount: 19, total: 119,
    currency: 'TND', deliveryAgentId: null, deliveryAgentName: null, receivedBy: null, receivedByName: null,
    receivedAt: null, deliveredAt: null, plannedDeliveryDate: null, confirmedDeliveryDate: null,
    asapPayment: false, deliveryRejectionReason: null, notes: '', version: 1, createdAt: '', updatedAt: '', items: [], events: []
  };

  beforeEach(() => {
    const orderSpy = jasmine.createSpyObj('OrderService', ['getById', 'getByReference', 'accept', 'acceptAsap', 'reject', 'cancel', 'downloadInvoice', 'printInvoice', 'getComments', 'addComment', 'getWithholdingCertificate', 'downloadWithholdingCertificatePdf', 'downloadWithholdingCertificateXml', 'markCertificateDeposited', 'updateExternalReference']);
    const disputeSpy = jasmine.createSpyObj('DisputeService', ['getByOrder', 'create']);
    const toastSpy = jasmine.createSpyObj('ToastService', ['success', 'error']);
    const confirmSpy = jasmine.createSpyObj('ConfirmDialogService', ['confirm']);
    confirmSpy.confirm.and.returnValue(of(true));
    const routerSpy = jasmine.createSpyObj('Router', ['navigate']);
    orderSpy.getById.and.returnValue(of(mockOrder));
    orderSpy.getByReference.and.returnValue(of(mockOrder));
    orderSpy.getComments.and.returnValue(of([]));
    disputeSpy.getByOrder.and.returnValue(of([]));

    TestBed.configureTestingModule({
    declarations: [ShopOrderDetailComponent, TranslateStubPipe, StatusLabelPipe, TimeAgoPipe],
    schemas: [NO_ERRORS_SCHEMA],
    imports: [],
    providers: [
        translateServiceProvider(),
        { provide: OrderService, useValue: orderSpy },
        { provide: DisputeService, useValue: disputeSpy },
        { provide: ToastService, useValue: toastSpy },
        { provide: ConfirmDialogService, useValue: confirmSpy },
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
    confirmDialog = TestBed.inject(ConfirmDialogService) as jasmine.SpyObj<ConfirmDialogService>;
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
      orderService.getByReference.and.returnValue(throwError(() => new Error('fail')));
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

  describe('withholding certificate', () => {
    it('should load certificate when order is DELIVERED', () => {
      orderService.getByReference.and.returnValue(of({ ...mockOrder, status: 'DELIVERED' }));
      orderService.getWithholdingCertificate.and.returnValue(of({ number: 'CRT-1' } as any));
      component.ngOnInit();
      expect(orderService.getWithholdingCertificate).toHaveBeenCalled();
      expect(component.certificate).toEqual({ number: 'CRT-1' } as any);
    });

    it('should not load certificate when order is DRAFT', () => {
      component.ngOnInit();
      expect(orderService.getWithholdingCertificate).not.toHaveBeenCalled();
      expect(component.certificate).toBeNull();
    });

    it('should download certificate PDF and XML and print invoice', () => {
      component.order = { ...mockOrder, id: 'ord-1' } as any;
      component.downloadCertificatePdf();
      expect(orderService.downloadWithholdingCertificatePdf).toHaveBeenCalledWith('ord-1');
      component.downloadCertificateXml();
      expect(orderService.downloadWithholdingCertificateXml).toHaveBeenCalledWith('ord-1');
      component.printInvoice();
      expect(orderService.printInvoice).toHaveBeenCalledWith('ord-1');
    });

    it('should mark certificate deposited', () => {
      orderService.markCertificateDeposited.and.returnValue(of({ number: 'CRT-1', status: 'DEPOSITED' } as any));
      component.order = { ...mockOrder, id: 'ord-1' } as any;
      component.tejReference = 'TEJ-1';
      component.markDeposited();
      expect(orderService.markCertificateDeposited).toHaveBeenCalledWith('ord-1', 'TEJ-1');
      expect(component.certificate).toEqual({ number: 'CRT-1', status: 'DEPOSITED' } as any);
      expect(component.showDepositForm).toBeFalse();
    });

    it('should save external reference', () => {
      orderService.updateExternalReference.and.returnValue(of({ ...mockOrder, externalReference: 'TTN-1' } as any));
      component.order = { ...mockOrder, id: 'ord-1' } as any;
      component.externalReference = 'TTN-1';
      component.saveExternalReference();
      expect(orderService.updateExternalReference).toHaveBeenCalledWith('ord-1', 'TTN-1');
      expect(component.order.externalReference).toBe('TTN-1');
      expect(component.showExtRefForm).toBeFalse();
    });

    it('should not save blank external reference', () => {
      component.order = { ...mockOrder, id: 'ord-1' } as any;
      component.externalReference = '  ';
      component.saveExternalReference();
      expect(orderService.updateExternalReference).not.toHaveBeenCalled();
    });
  });

  describe('rendering', () => {
    it('should render without events from backend (non-régression crash length)', () => {
      const { events, ...withoutEvents } = mockOrder as any;
      component.order = withoutEvents;
      component.loading = false;
      expect(() => fixture.detectChanges()).not.toThrow();
    });
  });

  describe('accept', () => {
    it('should accept order', () => {
      orderService.accept.and.returnValue(of({ ...mockOrder, status: 'ACCEPTED' }));
      component.order = mockOrder;
      component.accept();
      expect(toast.success).toHaveBeenCalledWith('ORDER_DETAIL.ACCEPTED_MSG');
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
      expect(toast.error).toHaveBeenCalledWith('ORDER_DETAIL.ERROR');
    });
  });

  describe('acceptAsap', () => {
    it('should accept ASAP', () => {
      orderService.acceptAsap.and.returnValue(of({ ...mockOrder, status: 'ACCEPTED' }));
      component.order = mockOrder;
      component.acceptAsap();
      expect(toast.success).toHaveBeenCalledWith('ORDER_DETAIL.ACCEPTED_ASAP_MSG');
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
      confirmDialog.confirm.and.returnValue(of(true));
      orderService.reject.and.returnValue(of({ ...mockOrder, status: 'REJECTED' }));
      component.order = mockOrder;
      component.reject();
      expect(toast.success).toHaveBeenCalledWith('ORDER_DETAIL.REJECTED_MSG');
    });

    it('should not reject when not confirmed', () => {
      confirmDialog.confirm.and.returnValue(of(false));
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
      confirmDialog.confirm.and.returnValue(of(true));
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
      expect(toast.success).toHaveBeenCalledWith('ORDER_DETAIL.CANCELLED_MSG');
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
      expect(component.actionLabel('ORDER_CREATED')).toBe('ORDER_DETAIL.ACTION_CREATED');
      expect(component.actionLabel('ORDER_CONFIRMED')).toBe('ORDER_DETAIL.ACTION_CONFIRMED');
      expect(component.actionLabel('ORDER_PREPARING')).toBe('ORDER_DETAIL.ACTION_PREPARING');
      expect(component.actionLabel('ORDER_READY_FOR_DELIVERY')).toBe('ORDER_DETAIL.ACTION_READY');
      expect(component.actionLabel('ORDER_IN_DELIVERY')).toBe('ORDER_DETAIL.ACTION_IN_DELIVERY');
      expect(component.actionLabel('ORDER_DELIVERED')).toBe('ORDER_DETAIL.ACTION_DELIVERED');
      expect(component.actionLabel('ORDER_ACCEPTED')).toBe('ORDER_DETAIL.ACTION_ACCEPTED');
      expect(component.actionLabel('ORDER_CANCELLED')).toBe('ORDER_DETAIL.ACTION_CANCELLED');
      expect(component.actionLabel('ORDER_ACCEPTED_ASAP')).toBe('ORDER_DETAIL.ACTION_ACCEPTED_ASAP');
      expect(component.actionLabel('ORDER_REJECTED')).toBe('ORDER_DETAIL.ACTION_REJECTED');
      expect(component.actionLabel('ORDER_DELIVERY_REJECTED')).toBe('ORDER_DETAIL.ACTION_DELIVERY_REJECTED');
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
